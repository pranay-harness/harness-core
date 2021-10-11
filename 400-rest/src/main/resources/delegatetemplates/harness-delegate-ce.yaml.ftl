# Be sure you have kubectl installed and credentials to access your
# kubernetes cluster.
#
# Edit harness-delegate.yaml to change namespace, set proxy settings, or to
# enter a delegate description.
#
# Install or replace the Harness Delegate:
#
#   kubectl apply -f harness-delegate.yaml
#
# Get pod names:
#
#   kubectl get pods -n harness-delegate
#
# See startup logs:
#
#   kubectl logs <pod-name> -n harness-delegate -f
#
# Run a shell in a pod:
#
#    kubectl exec <pod-name> -n harness-delegate -it -- bash
#
# Note: If you're installing more than one Kubernetes delegate then make sure the
# name is unique, keeping the 6 letter account identifier as part of the name. You
# can download again with a new name from the Harness > Setup > Installations
# page.



apiVersion: v1
kind: Namespace
metadata:
  name: harness-delegate

---

apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: ce-clusterrole
rules:
- apiGroups:
  - ""
  resources:
  - pods
  - nodes
  - nodes/proxy
  - events
  - namespaces
  - persistentvolumes
  - persistentvolumeclaims
  verbs:
  - get
  - list
  - watch
- apiGroups:
  - apps
  - extensions
  resources:
  - statefulsets
  - deployments
  - daemonsets
  - replicasets
  verbs:
  - get
  - list
  - watch
- apiGroups:
  - batch
  resources:
  - jobs
  - cronjobs
  verbs:
  - get
  - list
  - watch
- apiGroups:
  - metrics.k8s.io
  resources:
  - pods
  - nodes
  verbs:
  - get
  - list
- apiGroups:
  - storage.k8s.io
  resources:
  - storageclasses
  verbs:
  - get
  - list
  - watch

---

apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: ce-clusterrolebinding
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: ce-clusterrole
subjects:
- kind: ServiceAccount
  name: default
  namespace: harness-delegate

---

apiVersion: v1
kind: Secret
metadata:
  name: ${delegateName}-proxy
  namespace: harness-delegate
type: Opaque
data:
  # Enter base64 encoded username and password, if needed
  PROXY_USER: ""
  PROXY_PASSWORD: ""

---

apiVersion: apps/v1
kind: StatefulSet
metadata:
  labels:
    harness.io/app: harness-delegate
    harness.io/account: ${kubernetesAccountLabel}
    harness.io/name: ${delegateName}
  # Name must contain the six letter account identifier: ${kubernetesAccountLabel}
  name: ${delegateName}-${kubernetesAccountLabel}
  namespace: harness-delegate
spec:
  replicas: 1
  selector:
    matchLabels:
      harness.io/app: harness-delegate
      harness.io/account: ${kubernetesAccountLabel}
      harness.io/name: ${delegateName}
  serviceName: ""
  template:
    metadata:
      labels:
        harness.io/app: harness-delegate
        harness.io/account: ${kubernetesAccountLabel}
        harness.io/name: ${delegateName}
    spec:
      containers:
      - image: ${delegateDockerImage}
        imagePullPolicy: Always
        name: harness-delegate-instance
        resources:
          limits:
            cpu: "1"
            memory: "4Gi"
        readinessProbe:
          exec:
            command:
              - test
              - -s
              - delegate.log
          initialDelaySeconds: 20
          periodSeconds: 10
        livenessProbe:
          exec:
            command:
              - bash
              - -c
              - '[[ -e /opt/harness-delegate/msg/data/watcher-data && $(($(date +%s000) - $(grep heartbeat /opt/harness-delegate/msg/data/watcher-data | cut -d ":" -f 2 | cut -d "," -f 1))) -lt 300000 ]]'
          initialDelaySeconds: 240
          periodSeconds: 10
          failureThreshold: 2
        env:
        - name: JAVA_OPTS
          value: "-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=2 -Xms64M"
        - name: ACCOUNT_ID
          value: ${accountId}
        - name: ACCOUNT_SECRET
          value: ${accountSecret}
        - name: MANAGER_HOST_AND_PORT
          value: ${managerHostAndPort}
        - name: WATCHER_STORAGE_URL
          value: ${watcherStorageUrl}
        - name: WATCHER_CHECK_LOCATION
          value: ${watcherCheckLocation}
        - name: REMOTE_WATCHER_URL_CDN
          value: ${remoteWatcherUrlCdn}
        - name: DELEGATE_STORAGE_URL
          value: ${delegateStorageUrl}
        - name: DELEGATE_CHECK_LOCATION
          value: ${delegateCheckLocation}
        - name: DEPLOY_MODE
          value: ${deployMode}
        - name: DELEGATE_NAME
          value: ${delegateName}
        - name: DELEGATE_PROFILE
          value: "${delegateProfile}"
        - name: DELEGATE_TYPE
          value: "${delegateType}"
        - name: PROXY_HOST
          value: ""
        - name: PROXY_PORT
          value: ""
        - name: PROXY_SCHEME
          value: ""
        - name: NO_PROXY
          value: ""
        - name: PROXY_MANAGER
          value: "true"
        - name: PROXY_USER
          valueFrom:
            secretKeyRef:
              name: ${delegateName}-proxy
              key: PROXY_USER
        - name: PROXY_PASSWORD
          valueFrom:
            secretKeyRef:
              name: ${delegateName}-proxy
              key: PROXY_PASSWORD
        - name: POLL_FOR_TASKS
          value: "false"
        - name: HELM_DESIRED_VERSION
          value: ""
        - name: USE_CDN
          value: "${useCdn}"
        - name: CDN_URL
          value: ${cdnUrl}
        - name: JRE_VERSION
          value: ${jreVersion}
        - name: HELM3_PATH
          value: ""
        - name: HELM_PATH
          value: ""
        - name: KUSTOMIZE_PATH
          value: ""
        - name: KUBECTL_PATH
          value: ""
        - name: ENABlE_CE
          value: "${enableCE}"
        - name: VERSION_CHECK_DISABLED
          value: "${versionCheckDisabled}"
      restartPolicy: Always
