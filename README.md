Wings Project Dev environment setup instructions :

On MacOS

Prerequisities

1. Install Homebrew :
/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
2. Install Java download : brew cask install java
3. Install maven : brew install maven
4. Install mongo : brew install mongo
5. Install npm : brew install npm
6. Set up JAVA_HOME: create ~/.bash_profile file and add following line:
    export JAVA_HOME=$(/usr/libexec/java_home)

Build

1) Clone form git repository:  https://github.com/wings-software/wings
2) Start mongo db (mongod)
3) Go to wings directory and run mvn clean install

Note: On MacOS sierra, you may need fix for the slow java.net.InetAddress.getLocalHost() response problem as documented in this blog post (https://thoeni.io/post/macos-sierra-java/).

IDE Setup

1) Install IntelliJ community edition
2) Import wings portal as maven project
3) Import Code Style codeStyle/intellij-java-google-style.xml (Preferences->Editor->CodeStyle)
4) Import portal in intellij as maven project.
5) Import codeStyle/intellij-java-google-style.xml in intellij Settings/Editor/CodeStyle/Manage.

Run from IntelliJ
1) Run  API Server : Run 'WingsApplication' class  with following configurations.
    * Environment Variable: JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home
    * VM Args: -Xbootclasspath/p:&lt;Your Home Directory&gt;/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar
    * Program Args: server config.yml
    * Working Directory: $MODULE_DIR$
2) Run/Debug API Server : Run 'DelegateApplication' classp  with following configurations.
    * Environment Variable: JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home
    * VM Args: -Xbootclasspath/p:&lt;Your Home Directory&gt;/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar -Dversion=999.0.0
    * Program Args: config-delegate.yml
    * Working Directory: $MODULE_DIR$

Note:
1) To build UI Go to wings-ui and follow READ me instructions.

2) To apply database migrations run following command in dbmigrations folder:
    "mvn clean compile exec:java"
