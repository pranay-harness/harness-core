if [ $# -ne 3 ]
then
  echo "This script is used to run application on our servers by copying war files from staging folder and running java command."
  echo "Usage: $0 <Wings UI Host> <New Relic App Name> <Dropwizard metrics prefix>"
  echo "Wings UI Host: ci.wings.software, demo.wings.software"
  echo "New Relic App Name: 'CI Server', 'Demo Server'"
  echo "Dropwizard metrics prefix: ci, demo"
  exit 1
fi

pkill -f "capsule.jar config.yml"
/home/ubuntu/waitForJavaShutdown.sh "capsule.jar config.yml"

mkdir -p $HOME/backup; cp $HOME/rest-0.0.1-SNAPSHOT-capsule.jar $HOME/backup/rest-0.0.1-SNAPSHOT-capsule-$(date +%F-%H:%M).jar
mkdir -p $HOME/backup; cp $HOME/config.yml $HOME/backup/config-$(date +%F-%H:%M).yml
mkdir -p $HOME/backup; cp $HOME/portal.log $HOME/backup/portal-$(date +%F-%H:%M).log

cp $HOME/staging/rest-0.0.1-SNAPSHOT-capsule.jar $HOME
cp $HOME/staging/config.yml $HOME

sed -i 's/port: 9090/port: 3456/' config.yml
sed -i 's/keyStorePath: keystore.jks/keyStorePath: \/home\/ubuntu\/keystore.jks/' config.yml
sed -i 's/keyStorePassword: password/keyStorePassword: W!ngs@123/' config.yml
sed -i 's/certAlias: localhost/certAlias: java/' config.yml
sed -i "s/url: https:\/\/localhost:8000/url: https:\/\/${1}/" config.yml
sed -i "s/delegateMetadataUrl: http:\/\/wingsdelegates.s3-website-us-east-1.amazonaws.com\/delegateci.txt/delegateMetadataUrl: http:\/\/wingsdelegates.s3-website-us-east-1.amazonaws.com\/delegate${3}.txt/" config.yml
sed -i 's/9a3e6eac4dcdbdc41a93ca99100537df/4ac03b05674fc5c488e3b9b235078d5d/' config.yml
sed -i 's/carbon.hostedgraphite.com/ec2-34-205-52-18.compute-1.amazonaws.com/' config.yml
sed -i "s/prefix: server/prefix: ${3}/" config.yml

export HOSTNAME
NEW_RELIC_APP_NAME="${2}" nohup java -Xms1024m -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -Dfile.encoding=UTF-8 -jar $HOME/rest-0.0.1-SNAPSHOT-capsule.jar config.yml > portal.log 2>&1 &
cd $HOME/backup; ls -tQ *.yml| tail -n+4 | xargs --no-run-if-empty rm; ls -tQ *.jar| tail -n+4 | xargs --no-run-if-empty rm; ls -tQ *.log| tail -n+4 | xargs --no-run-if-empty rm
