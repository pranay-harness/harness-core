OLDDIR=$PWD
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR
mvn install:install-file -Dfile=mongo-java-distributed-lock-0.1.7.jar -DpomFile=mongo-java-distributed-lock-0.1.7.pom
mvn install:install-file -Dfile=hashed-wheel-timer-core-1.0.0-RC1.jar -DpomFile=hashed-wheel-timer-core-1.0.0-RC1.pom
mvn install:install-file -Dpackaging=pom -Dfile=hashed-wheel-timer-parent-1.0.0-RC1.pom -DpomFile=hashed-wheel-timer-parent-1.0.0-RC1.pom
cd $OLDDIR
