EngineHub Bot
=============
EngineHub Bot is the Discord bot to assist in moderating and providing support in the EngineHub Discord.

Downloading
-----------
The bot can be downloaded from [Jitpack](https://jitpack.io/com/github/EngineHub/EngineHub-Bot/-SNAPSHOT/EngineHub-Bot--SNAPSHOT.jar).

Compiling
---------
The project is written for Java 8 and our build process makes use of [Gradle](http://gradle.org/).
simply run:

    gradlew build

Dependencies are automatically handled by Gradle.

Running
-------
The main class is `org.enginehub.discord.EngineHubBot`.

The following is an example script, that will automatically update the bot during the automated restart.

```bash
#!/bin/bash

while true
do
    rm EngineHub-Bot.jar
    wget https://jitpack.io/com/github/EngineHub/EngineHub-Bot/-SNAPSHOT/EngineHub-Bot--SNAPSHOT.jar -O EngineHub-Bot.jar
    java -cp EngineHub-Bot.jar org.enginehub.discord.EngineHubBot
    sleep 10
done
```

Configuring
-----------
Upon initial startup, the program will generate multiple configuration files.
The most important setting to modify is `token` in `settings.conf`. 
This should be set to a Discord bot token, obtainable from the Discord website.

Contributing
------------
We happily accept contributions. The best way to do this is to fork the project
on GitHub, add your changes, and then submit a pull request. We'll look at it,
make comments, and merge it into the project if everything works out.

In addition, please ensure your code is compliant with the [Google Java
Conventions](https://google.github.io/styleguide/javaguide.html) to keep things neat and readable.

By submitting code, you agree to place to license your code under the [GPL License](https://raw.githubusercontent.com/EngineHub/EngineHub-Bot/master/LICENSE.txt).