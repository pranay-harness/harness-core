# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

WORKING_DIRECTORY=$(pwd)
MANIFEST_PATH="${serviceVariable.manifestPath}"
OVERRIDES_PATH="${serviceVariable.overridesPath}"


##########################################
##########################################
##
## MANIFEST
##
##########################################
##########################################

mkdir -p "$MANIFEST_PATH/templates"
cd "$MANIFEST_PATH"

read -r -d '' DEFAULT_VALUES <<- VALUES
name: ${workflow.variables.workloadName}
replicas: 1

image: ${artifact.metadata.image}
dockercfg: ${artifact.source.dockerconfig}

namespace: ${infra.kubernetes.namespace}
createNamespace: true

# Specify all environment variables to be added to the container.
# The following two maps, config and secrets, are put into a ConfigMap
# and a Secret, respectively.
# Both are added to the container environment in podSpec as envFrom source.
env:
  config:
    key1: value1
  secrets:
    key2: value2

overrides:
  value: default-override
  value1: default-override
  value2: default-override
  value3: default-override
  value4: default-override
VALUES

read -r -d '' DEPLOYMENT_MANIFEST <<- MANIFEST
apiVersion: v1
kind: ConfigMap
metadata:
  name: ${workflow.variables.workloadName}
data:
  key1: value1
---

apiVersion: v1
kind: Secret
metadata:
  name: ${workflow.variables.workloadName}
stringData:
  key2: value2
---

apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${workflow.variables.workloadName}-deployment
  labels:
    secret: ${secrets.getValue("custom-manifest-fn-test-secret")}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: {{.Values.name}}
      value: {{if ne "${workflow.variables.valueOverride}" ""}}${workflow.variables.valueOverride}{{ else }}{{ .Values.overrides.value }}{{end}}
      value1: {{if ne "${workflow.variables.value1Override}" ""}}${workflow.variables.value1Override}{{ else }}{{.Values.overrides.value1}}{{end}}
      value2: {{if ne "${workflow.variables.value2Override}" ""}}${workflow.variables.value2Override}{{ else }}{{.Values.overrides.value2}}{{end}}
      value3: {{if ne "${workflow.variables.value3Override}" ""}}${workflow.variables.value3Override}{{ else }}{{.Values.overrides.value3}}{{end}}
      value4: {{if ne "${workflow.variables.value4Override}" ""}}${workflow.variables.value4Override}{{ else }}{{.Values.overrides.value4}}{{end}}
  template:
    metadata:
      labels:
        app: {{.Values.name}}
        value: {{.Values.overrides.value}}
        value1: {{.Values.overrides.value1}}
        value2: {{.Values.overrides.value2}}
        value3: {{.Values.overrides.value3}}
        value4: {{.Values.overrides.value4}}
    spec:
      containers:
      - name: ${workflow.variables.workloadName}
        image: harness/todolist-sample:11
        envFrom:
        - configMapRef:
            name: ${workflow.variables.workloadName}
        - secretRef:
            name: ${workflow.variables.workloadName}
MANIFEST

read -r -d '' NAMESPACE_MANIFEST <<- MANIFEST
{{- if .Values.createNamespace}}
apiVersion: v1
kind: Namespace
metadata:
  name: {{.Values.namespace}}
{{- end}}
MANIFEST

echo "$DEFAULT_VALUES" > values.yaml
echo "$DEPLOYMENT_MANIFEST" > templates/deployment.yaml
echo "$NAMESPACE_MANIFEST" > templates/namespace.yaml


ABSOLUTE_PATH_MANIFEST="${serviceVariable.absolutePath}/${serviceVariable.manifestPath}"
if [ -f "$ABSOLUTE_PATH_MANIFEST" ]; then
  rm -r "$ABSOLUTE_PATH_MANIFEST"
fi

mkdir -p "$ABSOLUTE_PATH_MANIFEST/templates"

echo "$DEFAULT_VALUES" > "$ABSOLUTE_PATH_MANIFEST/values.yaml"
echo "$DEPLOYMENT_MANIFEST" > "$ABSOLUTE_PATH_MANIFEST/templates/deployment.yaml"
echo "$NAMESPACE_MANIFEST" > "$ABSOLUTE_PATH_MANIFEST/templates/namespace.yaml"

##########################################
##########################################
##
## ADDITIONAL OVERRIDES
##
##########################################
##########################################

cd "$WORKING_DIRECTORY"
mkdir -p "$OVERRIDES_PATH"
cd "$OVERRIDES_PATH"

read -r -d '' VALUES_OVERRIDE1 <<- OVERRIDE1
overrides:
  value1: values1-override
  value2: values1-override
OVERRIDE1

read -r -d '' VALUES_OVERRIDE2 <<- OVERRIDE2
overrides:
  value2: values2-override
  value3: values2-override
OVERRIDE2

echo "$VALUES_OVERRIDE1" > values1.yaml
echo "$VALUES_OVERRIDE2" > values2.yaml

ABSOLUTE_PATH_OVERRIDES="${serviceVariable.absolutePath}/${serviceVariable.overridesPath}"
if [ -f "$ABSOLUTE_PATH_OVERRIDES" ]; then
  rm -r "$ABSOLUTE_PATH_OVERRIDES"
fi

mkdir -p "$ABSOLUTE_PATH_OVERRIDES"

echo "$VALUES_OVERRIDE1" > "$ABSOLUTE_PATH_OVERRIDES/values1.yaml"
echo "$VALUES_OVERRIDE2" > "$ABSOLUTE_PATH_OVERRIDES/values2.yaml"
