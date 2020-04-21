set -x

mvn clean install -DskipTests -Dmaven.repo.local=/home/jenkins/maven-repositories/0

mvn ${MAVEN_ARGS} sortpom:sort -Dmaven.repo.local=/home/jenkins/maven-repositories/0 > /dev/null

mvn ${MAVEN_ARGS} -P protobuf clean generate-sources -Dmaven.repo.local=/home/jenkins/maven-repositories/0 > /dev/null

find . -iname "*.graphql" | xargs -L 1 prettier --write --print-width=120

find . -iname "*.java" | xargs clang-format -i

git diff --exit-code