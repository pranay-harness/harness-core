apiVersion: v1
kind: Namespace
metadata:
  name: harness-delegate

---

apiVersion: rbac.authorization.k8s.io/v1beta1
kind: ClusterRoleBinding
metadata:
  name: harness-delegate-cluster-admin
subjects:
  - kind: ServiceAccount
    name: default
    namespace: harness-delegate
roleRef:
  kind: ClusterRole
  name: cluster-admin
  apiGroup: rbac.authorization.k8s.io

---

apiVersion: apps/v1beta1
kind: StatefulSet
metadata:
  labels:
    harness.io/app: harness-delegate
    harness.io/account: ${kubernetesAccountLabel}
    harness.io/name: ${delegateName}
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
      - image: harness/delegate:latest
        imagePullPolicy: Always
        name: harness-delegate-instance
        resources:
          limits:
            cpu: "1"
            memory: "8Gi"
        env:
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
        - name: DELEGATE_STORAGE_URL
          value: ${delegateStorageUrl}
        - name: DELEGATE_CHECK_LOCATION
          value: ${delegateCheckLocation}
        - name: DEPLOY_MODE
          value: ${deployMode}
        - name: MULTI_VERSION
          value: "${multiVersion}"
        - name: DELEGATE_NAME
          value: ${delegateName}
        - name: DELEGATE_PROFILE
          value: "${delegateProfile}"
        - name: PROXY_HOST
          value: ""
        - name: PROXY_PORT
          value: ""
        - name: PROXY_SCHEME
          value: ""
        - name: NO_PROXY
          value: ""
        - name: POLL_FOR_TASKS
          value: "false"
        - name: HELM_DESIRED_VERSION
          value: ""
      restartPolicy: Always
