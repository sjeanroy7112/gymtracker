# Suivi Entraînement Android — version 4.5

Application Android native et hors connexion pour suivre les séances sur machines Matrix.

## Fonctions de la version 1.0

- Séances A et B préchargées avec les charges de Stéphane.
- Ajout et retrait de machines pendant une séance.
- Catalogue Matrix et machines personnalisées.
- Catalogue étendu à 30 machines et activités Matrix, classées par zones corporelles.
- Zones Pectoraux, Épaules, Bras, Dos, Abdominaux, Fessiers, Cuisses, Mollets et Cardio.
- Zone travaillée affichée dans les séances A et B, ainsi que sur chaque fiche machine.
- Photos réelles hors connexion pour toutes les nouvelles machines du catalogue.
- Modification de la charge, des répétitions, des séries et des notes.
- Chronomètre de récupération par exercice.
- Historique local des séances.
- Suppression individuelle d’une séance de l’historique, avec confirmation.
- Courbes par machine : charge, répétitions, séries et volume total.
- Poids utilisateur modifiable et courbe d’évolution au fil des séances.
- Repos uniformisé à 60 secondes pour toutes les machines.
- Comptage automatique d’une série à chaque lancement du repos.
- Bip sonore et vibration à la fin du compte à rebours.
- Mode running outdoor avec suivi GPS en arrière-plan.
- Distance, durée, allure moyenne, vitesse moyenne et pas.
- Pause/reprise et annonce vocale à chaque kilomètre.
- Historique distinct des sorties running avec suppression individuelle.
- Mode running indoor pour tapis de course, sans GPS.
- Vitesse du tapis réglable par pas de 0,5 km/h avec distance calculée en continu.
- Historique distinguant les sorties Outdoor et Tapis.
- Récupération après séance des pas Samsung Health/Galaxy Watch via Health Connect.
- Source des pas affichée dans l’historique : téléphone ou Samsung Health/montre.
- Récupération Samsung Health de la fréquence cardiaque moyenne, minimale et maximale.
- Récupération des calories, de la distance et de la vitesse de la montre lorsqu’elles sont disponibles.
- Nouvelle interface professionnelle avec accueil synthétique, cartes de séance et navigation inférieure.
- Refonte complète fidèle aux maquettes validées : accueil avec séance vedette et activités en grille.
- Écran de séance recomposé avec progression, repos, champs et commandes personnalisés.
- Une seule machine affichée à la fois, avec navigation précédente/suivante et accès direct par indicateurs.
- Compteurs modernes `− / valeur / +` pour la charge, les répétitions et les séries.
- Bouton principal « Série terminée » : incrément automatique, repos de 60 secondes et validation à l’objectif.
- Les machines à zéro série sont exclues de l’enregistrement et des courbes, y compris pour les anciennes séances.
- Samsung Health utilise désormais les calories actives, et non les calories totales de la journée.
- Contrôle de cohérence des pas et calories avec estimation de secours clairement indiquée.
- Photos réelles de machines Matrix intégrées hors connexion dans les écrans d’exercice.
- Les mêmes photos apparaissent dans le catalogue de sélection manuelle.
- Écran Paramètres pour modifier le nom, le poids et le temps de repos par défaut.
- Création du profil au premier lancement sur une nouvelle installation.
- Recherche et mémorisation d’une machine Matrix Bluetooth depuis les Paramètres.
- Recherche Bluetooth sans filtre afin de détecter les consoles Matrix qui n’annoncent pas le service FTMS pendant le scan.
- Accès unique « Musculation » avec choix de la séance enregistrée ou d’une séance libre.
- Séances A et B fournies comme modèles modifiables.
- Enregistrement d’une séance libre comme nouveau modèle nommé, réutilisable ensuite.
- Écran machine compact avec photo réduite et commandes visibles sans défilement sur smartphone portrait.
- Double appui sur la photo pour valider une série et lancer le chronomètre de repos.
- Flèches latérales et glissement horizontal pour passer d’une machine à l’autre.
- Machines triées en direct par puissance du signal avec indication Très proche, Proche ou Éloignée.
- Identifiant Bluetooth abrégé affiché pour différencier les consoles portant le même nom.
- Prise en charge du protocole standard Bluetooth FTMS pour tapis, elliptique et vélo Matrix compatibles.
- Lecture en direct de la vitesse, distance, cadence, puissance, calories et fréquence cardiaque lorsqu’elles sont diffusées.
- Bascule automatique entre le calcul manuel du tapis et la distance transmise par Matrix.
- Enregistrement de la source Matrix FTMS et des mesures disponibles dans l’historique.
- Navigation entre les machines par glissement horizontal.
- Suppression des boutons Précédente/Suivante afin de libérer de l’espace vertical.
- Défilement vertical conservé et pastilles toujours utilisables pour l’accès direct.
- Bouton « Rouvrir » sur chaque séance de musculation enregistrée.
- Reprise avec les charges, répétitions, séries, notes et statuts déjà saisis.
- Les machines encore non réalisées des programmes A/B sont automatiquement réintégrées.
- Le nouvel enregistrement remplace la séance rouverte afin d’éviter les doublons.
- Par sécurité, seules les séances enregistrées à la date du jour peuvent être rouvertes.
- Le bouton de reprise est masqué pour toutes les séances antérieures, avec un second contrôle lors de l’ouverture.
- Écran running avec distance mise en avant, statistiques en grille et commandes dédiées.
- Historique et graphiques harmonisés avec le nouveau langage visuel.
- Enregistrement des points GPS des nouveaux runnings outdoor.
- Aperçu hors connexion du parcours dans l’historique, avec marqueurs de départ et d’arrivée.
- Marquage « Fait » de chaque machine et compteur de progression.
- La zone utile s’arrête au-dessus de la barre de navigation Android.
- Écran de démarrage avec numéro de version.
- Illustration légère et spécifique pour chaque type de machine.
- Un clic sur le pictogramme affiche une vraie photo depuis le catalogue officiel Matrix Fitness.
- Aucune création de compte et aucun transfert de données.
- Au démarrage d’une séance, chaque machine reprend automatiquement la charge, les répétitions et l’objectif de séries de sa dernière utilisation enregistrée.
- Sans historique pour une machine, les valeurs du modèle ou du catalogue sont conservées.
- Chaque kilomètre indoor ou outdoor déclenche une annonce vocale avec la distance, le temps et la vitesse du dernier kilomètre, puis l’allure et la vitesse moyennes globales.


## Nouveautés 4.2

- Horodatage réel du début et de la fin des séances de musculation.
- Calcul et enregistrement de la durée totale, y compris après réouverture d'une séance du jour.
- Estimation prudente des calories de musculation à partir du poids, de la durée et de la densité de séries ; la valeur est clairement enregistrée comme estimée.
- Résumé enrichi dans l'historique : durée, calories estimées, nombre de séries et volume total (charge × répétitions × séries).
- Modèle de données préparé pour accueillir ensuite fréquence cardiaque et calories Health Connect / Galaxy Watch.
- Les anciennes séances restent lisibles ; les nouveaux indicateurs apparaissent lorsqu'ils sont disponibles.

## Installation

Pour mettre à jour une version 4.1 ou ultérieure sans perdre les données, signer l’APK 4.5 avec exactement la même clé que l’APK déjà installé. Ne pas désinstaller l’application avant la mise à jour.

Configuration minimale : Android 8.0 (API 26).


## Version 4.3
- Correction du bouton **Fermer** de la fenêtre de recherche Bluetooth : arrêt immédiat du scan et fermeture explicite de la fenêtre.
- Un appui sur un périphérique lance désormais l’**appairage Android**, puis la connexion GATT/FTMS une fois l’appairage terminé.
- Affichage de l’état appairé / non appairé dans la liste des périphériques.
- Timeout d’appairage de 30 secondes avec message d’erreur exploitable.

## Version 4.4

- Bouton de récupération Samsung Health ajouté aux séances de musculation enregistrées.
- Récupération des calories actives et des fréquences cardiaques moyenne, minimale et maximale sur l’intervalle exact de la séance.
- Les calories de la montre remplacent l’estimation uniquement lorsqu’elles sont disponibles et cohérentes.
- L’historique indique clairement « montre » ou « estimées ».
- L’estimation interne reste disponible comme solution de secours.

## Version 4.5

- Écran de recherche Matrix corrigé pour les Galaxy S24+ : bouton **Appairer** explicite sur chaque périphérique et bouton **Fermer** natif Android.
- Bouton **Terminer la machine** utilisable avant d’avoir atteint l’objectif de séries, avec possibilité de rouvrir la machine.
- Trophée de record pour une nouvelle meilleure charge ou un nouveau meilleur nombre de répétitions ; seules les séries réellement effectuées sont comparées.
- Trophée de record running outdoor lorsqu’un parcours suffisamment similaire à un parcours antérieur est couru plus vite.
- Message vocal d’encouragement à chaque kilomètre avec vitesse du dernier kilomètre et vitesse moyenne globale.
