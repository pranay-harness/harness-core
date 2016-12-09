pkill -f "capsule.jar config-delegate.yml"
/home/ubuntu/waitForJavaShutdown.sh "capsule.jar config-delegate.yml"

mkdir -p $HOME/backup; cp $HOME/delegate-0.0.1-SNAPSHOT-capsule.jar $HOME/backup/delegate-0.0.1-SNAPSHOT-capsule-$(date +%F-%H:%M).jar
mkdir -p $HOME/backup; cp $HOME/config-delegate.yml $HOME/backup/config-delegate-$(date +%F-%H:%M).yml
mkdir -p $HOME/backup; cp $HOME/delegate.log $HOME/backup/delegate-$(date +%F-%H:%M).log

cp $HOME/staging/delegate-0.0.1-SNAPSHOT-capsule.jar $HOME
cp $HOME/staging/config-delegate.yml $HOME

sed -i "s/accountId: kmpySmUISimoRrJL6NL73w/accountId: ${1}/" config-delegate.yml
sed -i "s/accountSecret: 2f6b0988b6fb3370073c3d0505baee59/accountSecret: ${2}/" config-delegate.yml
sed -i "s/managerUrl: https:\/\/localhost:9090\/api\//managerUrl: https:\/\/${3}\/api\//" config-delegate.yml
NEW_RELIC_APP_NAME="${4}" nohup java -Dfile.encoding=UTF-8 -jar $HOME/delegate-0.0.1-SNAPSHOT-capsule.jar config-delegate.yml > delegate.log 2>&1 &
cd $HOME/backup; ls -tQ *.yml| tail -n+4 | xargs --no-run-if-empty rm; ls -tQ *.jar| tail -n+4 | xargs --no-run-if-empty rm; ls -tQ *.log| tail -n+4 | xargs --no-run-if-empty rm
