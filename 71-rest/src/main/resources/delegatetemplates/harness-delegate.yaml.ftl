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
        - name: DELEGATE_NAME
          value: ${delegateName}
        - name: DELEGATE_PROFILE
          value: "${delegateProfile}"
        - name: MANAGER_TARGET
          value: "${managerTarget}"
        - name: MANAGER_AUTHORITY
          value: "${managerAuthority}"
<#if CCM_EVENT_COLLECTION??>
        - name: PUBLISH_TARGET
          value: "${publishTarget}"
        - name: PUBLISH_AUTHORITY
          value: "${publishAuthority}"
        - name: QUEUE_FILE_PATH
          value: "${queueFilePath}"
        - name: ENABLE_PERPETUAL_TASKS
          value: "${enablePerpetualTasks}"
</#if>
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
      restartPolicy: Always
