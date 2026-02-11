# Tabata Timer

Une application de chronomètre Tabata simple, élégante et efficace pour Android, développée avec Jetpack Compose.

## Fonctionnalités

*   **Configuration flexible** : Réglez librement les temps de préparation, de travail, de repos, le nombre de cycles et de séries.
*   **Cycles infinis** : Option pour un entraînement sans fin jusqu'à l'arrêt manuel.
*   **Saisie intuitive** : Modifiez les valeurs via des boutons +/- ou directement au clavier avec une interface dédiée.
*   **Utilisation en arrière-plan** : Le chronomètre continue de tourner même si l'application est réduite ou le téléphone verrouillé grâce à un service de premier plan.
*   **Optimisé pour le sport** :
    *   L'écran reste allumé pendant l'exercice.
    *   Affichage sur l'écran de verrouillage pour suivre son temps sans déverrouiller.
    *   Signal sonore (vrai son de cloche de boxe) et vibrations à chaque changement.
    *   Décompte sonore pour les 5 dernières secondes de chaque intervalle.
*   **Interface moderne** : Design soigné qui conserve ses couleurs (Bleu/Marine/Blanc) même en mode sombre pour une visibilité maximale en extérieur.
*   **Écran de fin** : Un écran de félicitations pour marquer la fin de votre effort.


## Technologies utilisées

*   **Kotlin**
*   **Jetpack Compose** (UI)
*   **Foreground Services** (Gestion du temps en arrière-plan)
*   **SoundPool** (Lecture audio haute performance)
*   **StateFlow** (Gestion réactive de l'état)

## Licence

Ce projet est sous licence MIT - voir le fichier [LICENSE](LICENSE) pour plus de détails.
