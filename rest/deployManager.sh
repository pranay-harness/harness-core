killall java; /home/ubuntu/waitForJavaShutdown.sh
mkdir -p $HOME/backup; cp $HOME/rest-0.0.1-SNAPSHOT-capsule.jar $HOME/backup/rest-0.0.1-SNAPSHOT-capsule-$(date +%F-%H:%M).jar
mkdir -p $HOME/backup; cp $HOME/config.yml $HOME/backup/config-$(date +%F-%H:%M).yml
mkdir -p $HOME/backup; cp $HOME/portal.log $HOME/backup/portal-$(date +%F-%H:%M).log

cp $HOME/staging/rest-0.0.1-SNAPSHOT-capsule.jar $HOME
cp $HOME/staging/config.yml $HOME

sed -i 's/port: 9090/port: 3456/' config.yml
sed -i 's/keyStorePath: keystore.jks/keyStorePath: \/home\/ubuntu\/keystore.jks/' config.yml
sed -i 's/keyStorePassword: password/keyStorePassword: W!ngs@123/' config.yml
sed -i 's/certAlias: selfsigned/certAlias: java/' config.yml
sed -i "s/url: https:\/\/localhost:8000/url: https:\/\/${1}/" config.yml
NEW_RELIC_APP_NAME="${2}" nohup java -Dfile.encoding=UTF-8 -jar $HOME/rest-0.0.1-SNAPSHOT-capsule.jar config.yml > portal.log 2>&1 &
cd $HOME/backup; ls -tQ *.yml| tail -n+4 | xargs --no-run-if-empty rm; ls -tQ *.jar| tail -n+4 | xargs --no-run-if-empty rm; ls -tQ *.log| tail -n+4 | xargs --no-run-if-empty rm
