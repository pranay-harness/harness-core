set -e
echo "${secrets.getValue("custom-manifest-fn-test-secret")}"
echo "${serviceVariable.manifestPath}"
helm version
helm create ${serviceVariable.manifestPath}
mkdir ${serviceVariable.overrideDir}
mv ${serviceVariable.manifestPath}/values.yaml ${serviceVariable.overridesPath1}