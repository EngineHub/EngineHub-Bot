Me4Bot
============
Me4Bot is a Discord bot providing basic administrative and music playing services.

Downloading
-----------
The bot can be downloaded from [Jitpack](https://jitpack.io/com/github/me4502/Me4Bot/-SNAPSHOT/Me4Bot--SNAPSHOT.jar).

Compiling
---------
The project is written for Java 8 and our build process makes use of [Gradle](http://gradle.org/).
simply run:

    gradlew build

Dependencies are automatically handled by Gradle.

Running
-------
The main class is `com.me4502.me4bot.discord.Me4Bot`.

The following is an example script, that will automatically update the bot during the automated restart.

```bash
#!/bin/bash

while true
do
    rm Me4Bot.jar
    wget https://jitpack.io/com/github/me4502/Me4Bot/-SNAPSHOT/Me4Bot--SNAPSHOT.jar -O Me4Bot.jar
    java -cp Me4Bot.jar com.me4502.me4bot.discord.Me4Bot
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

By submitting code, you agree to place to license your code under the [MIT License](https://raw.githubusercontent.com/me4502/Me4Bot/master/LICENSE.txt).