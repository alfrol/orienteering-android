# Orienteering app for Android

This is a simple orienteering app for Android. It was written as part of a course I took at the university.

During the develpment I tried to follow the MVVM desing patter, however please note that this is my second ever
app that I wrote for Android so there might be places where my descisions might seem weird etc.

To use the application the user is first asked to create an account and log in with it.

The purpose of the app is to help navigate people during the orienteering. The main screen includes a map where one
can start a session. When session is started a trail is drawn on the map as the user walks in real life. It's possible
to add two types of markers to the map:

1. Checkpoints - meant for saving permanent locations on the map and which can be visible later when viewing the completed session.
2. Waypoints - temporary markers which can be used for example for tracking the distance left to a certain point.

The main view also includes information about overall duration, distance travelled and average pace.
It's also possible to activate a real-time compass for easier navigation and toggle the map style between satellite and landscape.

There is a possibility to view the previous sessions which are pulled from the Room database. User can scroll through the list of sessions
and by selecting a certain session they are navigated to the same map screen but with the track and checkpoints visible.

User can also change some settings. For example change the style of the track segments.

Navigation in the app is made via Android Navigation Component. It also uses Dagger Hilt for dependency injection and
Google Maps component for map operations as well as other popular components and additional libraries for cleaner architecture.

This application is not completely finished as it still has some known bugs and places I am not particularly happy about. The app is
not published to the Marketpace as it was made for learning purposes.
