#Recording Alarm Clock

This app is a combination of alarm clock and audio recorder. The user can record audios and organize them via custom tags. The integrated alarm clock uses those tags to randomly select a recording with a tag of interest when it is time to wake up.
In short: Tomorrow when waking up I want to remember my last vacation in spain where I recorded some inspiring street musicians. So let's select the tag #spain17 ;-) I myself use this app to wake up from positive memories that I have recorded in the last few years.

##Screenshots

![Welcome Screen](/screenshots/welcome.png?raw=true "Alarm clock and recording view")
![Database Screen](/screenshots/context_tags.png?raw=true "Assign tags and access last played recording")
![Editing Screen](/screenshots/editing.png?raw=true "Create snippets of recordings (Memo)")
![Setting Screen](/screenshots/settings.png?raw=true "Sync with your Dropbox-Account")

##Install

1. Clone project
2. Get a custom App-Key for the Dropbox-API (you can create one [here](https://www.dropbox.com/developers/apps/create)) and insert it under res/values/api-keys.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE resources [
  <!ENTITY api_key_dropbox "YOUR-KEY">
]>
```
3. Compile and deploy!

##Technical overview
I started this project (apart from having the alarm clock of my dreams) to learn the basic concepts of native Android programming.
Since this is my first project some parts turned out pretty chaotic.

Some concepts used in this project:
- Dependency injection with Dagger2 in /injection
- Offline-Mode: All actions which need internet connection like upload,download are cached in a xml file. When connection is available the persistent actions are worked of like a stack
- ORM: Room Persistence Library from Google (JOIN operations, foreign keys etc.)
- Async programming with callbacks in cloud/tasks/
- Serialization of Java-Objects in /cloud/tasks/AsyncSyncFolderTask.java
- Abstract layer/Generics in /cloud/Cloud.java and implementation for different Cloud-Provider (for now only DropboxCloud.java)

##Data model
![Example UML](https://yuml.me/diagram/scruffy/class/[Recording]1++-1..*%3E[Memo],[Memo]*%3C%3E-*[Tag],class/[Time]*-1[Tag])
Of each audio recording you can create several small snippets, called Memos. Memos are defined by a certain start/stop position in the audio file and associated tags.

##Libraries/3rd Party
::GUI
- [OmRecorder](https://github.com/kailash09dabhi/OmRecorder)
- Fork of [audiowave-progressbar](https://github.com/alxrm/audiowave-progressbar)
- [TypeWriter](https://stackoverflow.com/a/6700718/9452450)

##Todos

- [x] Write ReadMe
- [ ] Include tests (Unit/Instrumented)