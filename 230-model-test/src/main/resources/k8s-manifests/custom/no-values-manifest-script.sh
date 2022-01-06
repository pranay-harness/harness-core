# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

WORKING_DIRECTORY=$(pwd)
MANIFEST_PATH="${serviceVariable.manifestPath}"
OVERRIDES_PATH="${serviceVariable.overridesPath}"

mkdir -p "$MANIFEST_PATH/templates"
cd "$MANIFEST_PATH"

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
      app: ${workflow.variables.workloadName}
  template:
    metadata:
      labels:
        app: ${workflow.variables.workloadName}
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
apiVersion: v1
kind: Namespace
metadata:
  name: ${infra.kubernetes.namespace}
MANIFEST

echo "$DEPLOYMENT_MANIFEST" > templates/deployment.yaml
echo "$NAMESPACE_MANIFEST" > templates/namespace.yaml
