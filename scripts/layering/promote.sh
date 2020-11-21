FROM_MODULE=$1
FROM_PACKAGE=$2
FROM_PACKAGE_DIR=${FROM_PACKAGE//./\/}

TO_MODULE=$3
TO_PACKAGE=$4
TO_PACKAGE_DIR=${TO_PACKAGE//./\/}

PROJECT=$5

mvn clean install -pl $FROM_MODULE --also-make -DskipTests
CLASSES=`find $FROM_MODULE/target/classes/$FROM_PACKAGE_DIR -iname "*.class" | grep -v "[$]"`

for CLASS_PATH in $CLASSES
do
  COUNT=`jdeps -v -cp $FROM_MODULE/target/classes $CLASS_PATH | grep " -> software.wings\| -> io.harness" | grep -v "not found" | wc -l`
  if [ $COUNT -eq 0 ]
  then
    if [ -z "$TO_MODULE" ]
    then
      echo $CLASS_PATH
    else
      break
    fi
  fi
done

if [ -z "$TO_MODULE" ]
then
  exit 0
fi

SOURCE=`echo $CLASS_PATH | sed 's;target/classes;src/main/java;g' | sed 's/\.class/.java/g'`
TARGET=`echo $SOURCE | sed "s/$FROM_MODULE/$TO_MODULE/g" | sed "s;$FROM_PACKAGE_DIR;$TO_PACKAGE_DIR;g" `

mkdir -p `dirname $TARGET`
mv $SOURCE $TARGET

sed -i "" "s/package $FROM_PACKAGE/package $TO_PACKAGE/g" $TARGET

SOURCE_JAVA_CLASS=`echo $CLASS_PATH | sed "s;$FROM_MODULE/target/classes/;;g" | sed 's/\.class//g' | sed 's;/;.;g'`
TARGET_JAVA_CLASS=`echo $SOURCE_JAVA_CLASS | sed "s;$FROM_PACKAGE;$TO_PACKAGE;g"`

echo $SOURCE_JAVA_CLASS
echo $TARGET_JAVA_CLASS

find . -type f -name '*.java' | cut -c 3- | xargs -L 1 sed -i "" "s/$SOURCE_JAVA_CLASS/$TARGET_JAVA_CLASS/g"

git checkout -b promote-$SOURCE_JAVA_CLASS
git add .
git commit -am "[$PROJECT-0]: promote $SOURCE_JAVA_CLASS"