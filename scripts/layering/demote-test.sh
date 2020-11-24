set -ex

FROM_MODULE=$1
TO_MODULE=$2
PACKAGE=$3
PACKAGE_DIR=${PACKAGE//./\/}

PROJECT=$4

mvn clean install -DskipTests -Dmaven.repo.local=$(pwd)/.m2

find $FROM_MODULE/target/test-classes/ -iname "*.class" | cut -c 30- | rev | cut -c 7- | rev | cut -f1 -d"$" | tr "/" "." | sort | uniq > $FROM_MODULE/target/file1.txt
jdeps -v 71-rest/target/rest-tests.jar | grep "rest-tests.jar" | awk ' { print $3 } ' | cut -f1 -d"$" | sort | uniq > $FROM_MODULE/target/file2.txt

IFS=
CLASSES=`diff $FROM_MODULE/target/file1.txt $FROM_MODULE/target/file2.txt | grep "< " | cut -c 3- | grep $PACKAGE`

if [ -z "$TO_MODULE" ]
then
  echo $CLASSES
  exit 0
fi

if [ -z "$CLASSES" ]
then
  echo "no more classes"
  exit 1
fi

JAVA_CLASS=`echo "$CLASSES" | head -n 1`

SOURCE=$FROM_MODULE/src/test/java/`echo $JAVA_CLASS | sed 's;[.];/;g'`.java
TARGET=`echo $SOURCE | sed "s/$FROM_MODULE/$TO_MODULE/g"`

mkdir -p `dirname $TARGET`
mv $SOURCE $TARGET

git checkout -b demote-$JAVA_CLASS
git add .
git commit -am "[$PROJECT-0]: demote $JAVA_CLASS"