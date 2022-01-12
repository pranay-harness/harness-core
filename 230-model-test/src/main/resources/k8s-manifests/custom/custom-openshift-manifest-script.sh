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
## TEMPLATE
##
##########################################
##########################################

mkdir -p "$MANIFEST_PATH"
cd "$MANIFEST_PATH"

read -r -d '' TEMPLATE_MANIFEST <<- TEMPLATE
apiVersion: v1
kind: Template
metadata:
  name: ${workflow.variables.workloadName}-template
  annotations:
    description: "Description"
objects:
- apiVersion: v1
  kind: ConfigMap
  metadata:
    name: \${WORKLOAD_NAME}
  data:
    value: \${CONFIGURATION}
- apiVersion: v1
  kind: Secret
  metadata:
    name: \${WORKLOAD_NAME}
  stringData:
    value: \${SECRET}
- apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: \${WORKLOAD_NAME}-deployment
    labels:
      secret: ${secrets.getValue("custom-manifest-fn-test-secret")}
  spec:
    replicas: 1
    selector:
      matchLabels:
        app: \${WORKLOAD_NAME}
        param: ${workflow.variables.valueOverride}
        param1: ${workflow.variables.value1Override}
        param2: ${workflow.variables.value2Override}
        param3: ${workflow.variables.value3Override}
        param4: ${workflow.variables.value4Override}

    template:
      metadata:
        labels:
          app: \${WORKLOAD_NAME}
          param: \${PARAM}
          param1: \${PARAM1}
          param2: \${PARAM2}
          param3: \${PARAM3}
          param4: \${PARAM4}
      spec:
        containers:
        - name: \${WORKLOAD_NAME}
          image:  harness/todolist-sample:11
          envFrom:
          - configMapRef:
              name: \${WORKLOAD_NAME}
          - secretRef:
              name: \${WORKLOAD_NAME}
parameters:
- name: WORKLOAD_NAME
  description: Workload name
  value: ${workflow.variables.workloadName}
- name: CONFIGURATION
  description: Configuration value
  value: Some configuration value
- name: SECRET
  description: Secret vvalue
  value: Some secret value
- name: PARAM
  description: Param value
  value: default-override
- name: PARAM1
  description: Param value
  value: default-override
- name: PARAM2
  description: Param value
  value: default-override
- name: PARAM3
  description: Param value
  value: default-override
- name: PARAM4
  description: Param value
  value: default-override
TEMPLATE

echo "$TEMPLATE_MANIFEST" > template.yaml

ABSOLUTE_PATH_MANIFEST="${serviceVariable.absolutePath}/${serviceVariable.manifestPath}"
if [ -f "$ABSOLUTE_PATH_MANIFEST" ]; then
  rm -r "$ABSOLUTE_PATH_MANIFEST"
fi

mkdir -p "$ABSOLUTE_PATH_MANIFEST"

echo "$TEMPLATE_MANIFEST" > "$ABSOLUTE_PATH_MANIFEST/template.yaml"

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

read -r -d '' PARAMS_OVERRIDE1 <<- OVERRIDE1
PARAM1: values1-override
PARAM2: values1-override
OVERRIDE1

read -r -d '' PARAMS_OVERRIDE2 <<- OVERRIDE2
PARAM2: values2-override
PARAM3: values2-override
OVERRIDE2

echo "$PARAMS_OVERRIDE1" > params1
echo "$PARAMS_OVERRIDE2" > params2


ABSOLUTE_PATH_OVERRIDES="${serviceVariable.absolutePath}/${serviceVariable.overridesPath}"
if [ -f "$ABSOLUTE_PATH_OVERRIDES" ]; then
  rm -r "$ABSOLUTE_PATH_OVERRIDES"
fi

mkdir -p "$ABSOLUTE_PATH_OVERRIDES"
echo "$PARAMS_OVERRIDE1" > "$ABSOLUTE_PATH_OVERRIDES/params1"
echo "$PARAMS_OVERRIDE2" > "$ABSOLUTE_PATH_OVERRIDES/params2"
