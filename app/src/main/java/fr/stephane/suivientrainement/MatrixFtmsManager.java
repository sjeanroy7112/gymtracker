package fr.stephane.suivientrainement;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@SuppressLint("MissingPermission")
public final class MatrixFtmsManager {
    public interface ScanListener { void onDevice(BluetoothDevice device,int rssi); void onFinished(); }
    public interface DataListener { void onStatus(String status); void onData(); }
    public interface PairListener { void onPairedAndConnecting(); void onAlreadyPaired(); void onPairingFailed(String message); }
    private static final UUID FTMS=uuid("1826"),TREADMILL=uuid("2acd"),CROSS_TRAINER=uuid("2ace"),INDOOR_BIKE=uuid("2ad2"),CCC=uuid("2902");
    private static final String PREFS="gym_tracker",ADDRESS="matrix_ble_address",NAME="matrix_ble_name";
    private static MatrixFtmsManager instance;
    private final Context context;private final BluetoothManager bluetoothManager;private final Handler main=new Handler(Looper.getMainLooper());
    private BluetoothLeScanner scanner;private ScanCallback scanCallback;private BluetoothGatt gatt;private DataListener listener;private double distanceBase=-1;
    public String status="Non connecté",deviceName="";public double speedKmh=0,distanceMeters=0,cadence=0,power=0,calories=0;public int heartRate=0;
    private MatrixFtmsManager(Context c){context=c.getApplicationContext();bluetoothManager=(BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);SharedPreferences p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);deviceName=p.getString(NAME,"");}
    public static synchronized MatrixFtmsManager get(Context c){if(instance==null)instance=new MatrixFtmsManager(c);return instance;}
    public void setListener(DataListener l){listener=l;if(l!=null)l.onStatus(status);}
    public String savedName(){return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(NAME,"");}
    public boolean hasSavedDevice(){return !context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(ADDRESS,"").isEmpty();}
    public boolean isConnected(){return gatt!=null&&RunningService.ftmsConnected;}
    public void beginSession(){distanceBase=-1;distanceMeters=0;calories=0;cadence=0;power=0;heartRate=0;}
    public void forget(){disconnect();context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove(ADDRESS).remove(NAME).apply();deviceName="";setStatus("Non configuré");}
    public void scan(final ScanListener callback){BluetoothAdapter adapter=bluetoothManager==null?null:bluetoothManager.getAdapter();if(adapter==null||!adapter.isEnabled()){setStatus("Bluetooth désactivé");callback.onFinished();return;}stopScan();scanner=adapter.getBluetoothLeScanner();if(scanner==null){callback.onFinished();return;}final Map<String,Integer> signals=new LinkedHashMap<>();scanCallback=new ScanCallback(){@Override public void onScanResult(int type,ScanResult result){BluetoothDevice d=result.getDevice();String address=d.getAddress();int rssi=result.getRssi();Integer previous=signals.put(address,rssi);if(previous==null||Math.abs(previous-rssi)>=3)callback.onDevice(d,rssi);}};setStatus("Recherche Bluetooth…");scanner.startScan(scanCallback);main.postDelayed(()->{stopScan();callback.onFinished();setStatus("Recherche terminée");},10000);}
    public void stopScan(){if(scanner!=null&&scanCallback!=null)try{scanner.stopScan(scanCallback);}catch(Exception ignored){}scanCallback=null;}
    public void connectSaved(){String address=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(ADDRESS,"");if(address.isEmpty())return;try{connect(bluetoothManager.getAdapter().getRemoteDevice(address));}catch(Exception e){setStatus("Machine introuvable");}}
    public String bondStateLabel(BluetoothDevice device){try{int state=device.getBondState();if(state==BluetoothDevice.BOND_BONDED)return "appairée";if(state==BluetoothDevice.BOND_BONDING)return "appairage…";return "non appairée";}catch(Exception e){return "état inconnu";}}
    public void pairAndConnect(final BluetoothDevice device,final PairListener callback){stopScan();int state;try{state=device.getBondState();}catch(Exception e){callback.onPairingFailed("Impossible de lire l’état d’appairage Bluetooth.");return;}if(state==BluetoothDevice.BOND_BONDED){connect(device);callback.onAlreadyPaired();return;}setStatus("Appairage avec "+safeName(device)+"…");boolean started;try{started=device.createBond();}catch(Exception e){callback.onPairingFailed("Android n’a pas pu lancer l’appairage : "+e.getMessage());return;}if(!started&&device.getBondState()!=BluetoothDevice.BOND_BONDING){callback.onPairingFailed("L’appairage n’a pas pu être lancé. Vérifie que la machine est en mode Bluetooth.");return;}final long deadline=System.currentTimeMillis()+30000;final Runnable waiter=new Runnable(){@Override public void run(){int current;try{current=device.getBondState();}catch(Exception e){callback.onPairingFailed("Erreur pendant l’appairage Bluetooth.");return;}if(current==BluetoothDevice.BOND_BONDED){connect(device);callback.onPairedAndConnecting();return;}if(System.currentTimeMillis()>=deadline){setStatus("Appairage expiré");callback.onPairingFailed("Délai d’appairage dépassé. Relance la recherche et vérifie que la machine est prête à être appairée.");return;}main.postDelayed(this,500);}};main.postDelayed(waiter,500);}
    private String safeName(BluetoothDevice device){try{String n=device.getName();return n==null||n.trim().isEmpty()?"la machine":n;}catch(Exception e){return "la machine";}}
    public void connect(BluetoothDevice device){stopScan();disconnect();deviceName=device.getName()==null?"Matrix FTMS":device.getName();context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(ADDRESS,device.getAddress()).putString(NAME,deviceName).apply();setStatus("Connexion à "+deviceName+"…");distanceBase=-1;gatt=device.connectGatt(context,false,gattCallback,BluetoothDevice.TRANSPORT_LE);}
    public void disconnect(){if(gatt!=null){try{gatt.disconnect();gatt.close();}catch(Exception ignored){}gatt=null;}RunningService.ftmsConnected=false;distanceBase=-1;}
    private final BluetoothGattCallback gattCallback=new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt g,int statusCode,int newState){if(newState==BluetoothProfile.STATE_CONNECTED){setStatus("Connecté à "+deviceName);g.discoverServices();}else{RunningService.ftmsConnected=false;setStatus("Déconnecté");}}
        @Override public void onServicesDiscovered(BluetoothGatt g,int statusCode){BluetoothGattService service=g.getService(FTMS);if(service==null){setStatus("Bluetooth détecté, mais FTMS absent");return;}BluetoothGattCharacteristic data=service.getCharacteristic(TREADMILL);if(data==null)data=service.getCharacteristic(CROSS_TRAINER);if(data==null)data=service.getCharacteristic(INDOOR_BIKE);if(data==null){setStatus("Données FTMS non reconnues");return;}g.setCharacteristicNotification(data,true);BluetoothGattDescriptor descriptor=data.getDescriptor(CCC);if(descriptor!=null){descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);g.writeDescriptor(descriptor);}RunningService.ftmsConnected=true;setStatus("Données Matrix en direct");}
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){parse(c.getUuid(),c.getValue());}
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] value){parse(c.getUuid(),value);}
    };
    private void parse(UUID type,byte[] bytes){if(bytes==null||bytes.length<4)return;ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);int flags=u16(b);double rawDistance=-1;if(type.equals(TREADMILL)){boolean more=(flags&1)!=0;if(!more&&b.remaining()>=2)speedKmh=u16(b)/100.0;if((flags&2)!=0&&b.remaining()>=2)b.position(b.position()+2);if((flags&4)!=0&&b.remaining()>=3)rawDistance=u24(b);if((flags&8)!=0&&b.remaining()>=4)b.position(b.position()+4);if((flags&16)!=0&&b.remaining()>=4)b.position(b.position()+4);if((flags&32)!=0&&b.remaining()>=1)b.position(b.position()+1);if((flags&64)!=0&&b.remaining()>=1)b.position(b.position()+1);if((flags&128)!=0&&b.remaining()>=5){calories=u16(b);b.position(b.position()+3);}if((flags&256)!=0&&b.remaining()>=1)heartRate=b.get()&255;
        }else{boolean more=(flags&1)!=0;if(!more&&b.remaining()>=2)speedKmh=u16(b)/100.0;if((flags&2)!=0&&b.remaining()>=2)b.position(b.position()+2);if((flags&4)!=0&&b.remaining()>=2)cadence=u16(b)/2.0;if((flags&8)!=0&&b.remaining()>=2)b.position(b.position()+2);int distanceFlag=type.equals(INDOOR_BIKE)?16:4;if((flags&distanceFlag)!=0&&b.remaining()>=3)rawDistance=u24(b);if(type.equals(INDOOR_BIKE)&&(flags&64)!=0&&b.remaining()>=2)power=b.getShort();}
        if(rawDistance>=0){if(distanceBase<0)distanceBase=rawDistance;distanceMeters=Math.max(0,rawDistance-distanceBase);}RunningService.treadmillSpeedKmh=speedKmh;if(RunningService.active&&RunningService.indoor&&rawDistance>=0)RunningService.distanceMeters=distanceMeters;RunningService.machineCadence=cadence;RunningService.machinePower=power;RunningService.machineCalories=calories;RunningService.machineHeartRate=heartRate;notifyData();}
    private void setStatus(String value){status=value;main.post(()->{if(listener!=null)listener.onStatus(value);});}
    private void notifyData(){main.post(()->{if(listener!=null)listener.onData();});}
    private static int u16(ByteBuffer b){return b.getShort()&65535;}private static int u24(ByteBuffer b){return (b.get()&255)|((b.get()&255)<<8)|((b.get()&255)<<16);}
    private static UUID uuid(String shortId){return UUID.fromString("0000"+shortId+"-0000-1000-8000-00805f9b34fb");}
}
