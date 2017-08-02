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

7. Go to http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html. Accept the license agreement and download the files. Unzip the files. Copy the two jars to `$JAVA_HOME/jre/lib/security` (you'll probably need to use sudo).

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

### Run Harness without IDE (especially for the UI development)
1) Start server : Replace the <Your Home Directory> with the appropriate value(such as /home/rishi) and run following commands.

`export HOSTNAME`

`mvn clean install -DskipTests && java -Xms1024m -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Xbootclasspath/p:<Your Home Directory>/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar -Dfile.encoding=UTF-8 -jar rest/target/rest-0.0.1-SNAPSHOT-capsule.jar rest/config.yml > portal.log &`

2) Run DataGenUtil: Open a new terminal and run following command :

`mvn test -pl rest -Dtest=software.wings.integration.DataGenUtil`


3) Start Delegate : Open a new terminal and navigate to the same directory. And run following command:

`java -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -jar delegate/target/delegate-0.0.1-SNAPSHOT-capsule.jar delegate/config-delegate.yml &`


### IDE Setup

1) Install IntelliJ community edition
2) Import wings portal as maven project
3) Import Code Style tools/src/main/resources/do-not-use/intellij-java-google-style.xml (Preferences->Editor->CodeStyle)

### Run from IntelliJ
1) Create the API Server application - "WingsApplication":  
[Run -> Edit Configurations...]

    * Add new Application:  
        Use the "+" on the left to add a new application. Call it "WingsApplication"
    
    * Set Main class:   
        'WingsApplication' class (found at software.wings.app.WingsApplication) with the following configurations.
    
    * VM Options:  
        `-Xbootclasspath/p:<Your Home Directory>/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar`
    
    * Program Arguments:  
        `server config.yml`
    
    * Working Directory:  
        `$MODULE_DIR$`
    
    * Environment Variable:   
        `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home`
    
    * Use classpath of module:  
        rest
    
    * JRE:  
        Default (1.8 - SDK of 'rest' module)
    
    * Ensure [File -> Project Structure -> Project SDK] "java version" is 1.8.0_121.
    * Ensure [IntelliJ IDEA -> Preferences -> Build, Execution, Deployment -> Compile -> Java Compiler -> Module] "Target Bytecode Version" is 1.8 for all modules.

2) Create the "DelegateApplication":  
[Run -> Edit Configurations...]
    * Add new Application:  
        Use the "+" on the left to add a new application. Call it "DelegateApplication"
    
    * Set Main class:   
        'DelegateApplication' class (found at software.wings.delegate.app.WingsApplication) with the following configurations.
    
    * VM Options:  
        `-Xbootclasspath/p:<Your Home Directory>/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar -Dversion=999.0.0`
    
    * Program Arguments:  
        `config-delegate.yml`
    
    * Working Directory:  
        `$MODULE_DIR$`
    
    * Environment Variable:  
        `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home`
    
    * Use classpath of module:  
        delegate
        
    * JRE:  
        Default (1.8 - SDK of 'delegate' module)
        
### Before you can use the client:

1) Make sure your mongodb is running first.  

2) Run API Server (WingsApplication): [Run -> Run... -> WimngsApplication]   

3) From within the IDE, run `rest/src/test/java/software/wings/integration/DataGenUtil.java` and  

4) `rest/src/test/java/software/wings/service/impl/RoleRefreshUtil.java` to create the default users and roles.   

5) 2) Run DelegateApplication: [Run -> Run... -> DelegateApplication]  

The admin username and password are in BaseIntegrationTest.java.  

### Note:
1) To build UI Go to wingsui and follow READ me instructions.

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
