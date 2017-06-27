# Wings Project Dev environment setup instructions :

## On MacOS

### Prerequisities

1. Install Homebrew :

    `/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"`
2. Install Java download :

    `brew cask install java`
3. Install maven :

    `brew install maven`
4. Install and start mongodb :

    `brew install mongo && brew services start mongodb`
5. Install npm :
    `brew install npm`

6. Set up JAVA_HOME: create ~/.bash_profile file and add following line:

   `export JAVA_HOME=$(/usr/libexec/java_home)`

### Build

1) Clone form git repository: https://github.com/wings-software/wings

   (Optional) Follow https://help.github.com/articles/adding-a-new-ssh-key-to-your-github-account/
   to setup your SSH keys. You can then use SSH to interact with git

2) Start mongo db (mongod)
   You may need to create a blank mongo db directory to do this. If mongod fails:
   `sudo mkdir /data`
   `sudo mkdir /data/db`
   `sudo chmod 777 /data/db`
   You can also do
   `sudo mkdir -p /data/db`
   `sudo chown -R <user name> /data`
3) Go to wings directory and run

    `mvn clean install`

Note: On MacOS sierra, you may need fix for the slow java.net.InetAddress.getLocalHost() response problem as documented in this blog post (https://thoeni.io/post/macos-sierra-java/).

### IDE Setup

1) Install IntelliJ community edition
2) Import wings portal as maven project
3) Import Code Style tools/src/main/resources/do-not-use/intellij-java-google-style.xml (Preferences->Editor->CodeStyle)

### Run from IntelliJ
1) Run API Server : Make sure your mongodb is running first. Run 'WingsApplication' class (found at wings/rest/target/classes/software/wings/app/WingsApplication.class) with the following configurations.
    * Environment Variable:

        `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home`
    * VM Args:

        `-Xbootclasspath/p:<Your Home Directory>/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar`
    * Program Args:

        `server config.yml`
    * Working Directory:

        `$MODULE_DIR$`
    * Ensure [IntelliJ -> Project Structure -> Project SDK] "java version" is 1.8.0_121.
    * Ensure [IntelliJ -> Preferences -> Java Compiler -> Module] "Target Bytecode Version" is 1.8 for all modules.

2) Run/Debug API Server : Run 'DelegateApplication' class  with the following configurations.
    * Environment Variable:

        `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home`
    * VM Args:

        `-Xbootclasspath/p:<Your Home Directory>/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar -Dversion=999.0.0`
    * Program Args:

        `config-delegate.yml`
    * Working Directory:

        `$MODULE_DIR$`

### Before you can use the client:
1) From within the IDE, run `rest/src/test/java/software/wings/integration/DataGenUtil.java` and `rest/src/test/java/software/wings/service/impl/RoleRefreshUtil.java` to create the default users and roles. The admin username and password are in DataGenUtil.

2) Go to http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html. Accept the license agreement and download the files. Unzip the files. Copy the two jars to `$JAVA_HOME/jre/lib/security` (you'll probably need to use sudo).

### Note:
1) To build UI Go to wings-ui and follow READ me instructions.

2) To apply database migrations run following command in dbmigrations folder:

    ```mvn clean compile exec:java```

### Common problems:
* If you get an error about missing build.properties when you start the server, do a mvn clean install.
* If you go to https://localhost:8000/#/login and don't see content, go to https://localhost:8181/#/login to enable the certificate then try again.
* If still face not able to login then got to https://localhost:9090/api/version and enable certificate and try again.

### Python
* Refer to the readme under python/splunk_intelligence

### Troubleshooting
https://github.com/wings-software/wings/wiki/Troubleshooting-running-java-process
