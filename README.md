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

## Screenshots

<img src="https://user-images.githubusercontent.com/43606916/131249736-7bd32c38-0d85-41d6-b9dc-3c53e3da5119.jpg" width="250" alt="Welcome screen" />
<img src="https://user-images.githubusercontent.com/43606916/131249879-5f54b801-3497-411a-a048-8ade0596e985.jpg" width="250" alt="Login screen" />
<img src="https://user-images.githubusercontent.com/43606916/131249889-0717699d-64a4-44ea-89af-da675909eb50.jpg" width="250" alt="Registration screen" />
<img src="https://user-images.githubusercontent.com/43606916/131249980-72f107f8-0962-4e62-bdde-b6a8eed216b7.jpg" width="250" alt="Menu screen" />
<img src="https://user-images.githubusercontent.com/43606916/131249982-47755d22-11ab-48b4-b1e1-f0b392a3f9fc.jpg" width="250" alt="Settings dialog" />
<img src="https://user-images.githubusercontent.com/43606916/131249983-08982ae9-5092-4eb4-9bb8-287cb7311a04.jpg" width="250" alt="History screen" />
<img src="https://user-images.githubusercontent.com/43606916/131249987-3fb83cf3-605b-45ae-b628-e68dcceca90e.jpg" width="250" alt="Main map screen" />
<img src="https://user-images.githubusercontent.com/43606916/131249993-1957d30c-2400-4e66-a81c-f28e539d96c4.jpg" width="250" alt="Main map screen with expanded statistics" />
<img src="https://user-images.githubusercontent.com/43606916/131249995-dfad5f1a-0b70-4e51-981b-56732a0dce4a.jpg" width="250" alt="Main map screen with collapsed statistics" />
<img src="https://user-images.githubusercontent.com/43606916/131249997-1b2f7b72-9b4f-4150-84b8-28585bf8486a.jpg" width="250" alt="Main map screen with opened compass" />
<img src="https://user-images.githubusercontent.com/43606916/131250000-859eceff-09fd-49f3-8a8f-43dd205fbb35.jpg" width="250" alt="Session from history screen" />
