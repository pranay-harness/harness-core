apiVersion: v1
kind: Namespace
metadata:
  name: harness-delegate-ng

---

apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: harness-delegate-ng-cluster-admin
subjects:
  - kind: ServiceAccount
    name: default
    namespace: harness-delegate-ng
roleRef:
  kind: ClusterRole
  name: cluster-admin
  apiGroup: rbac.authorization.k8s.io

---

apiVersion: v1
kind: Secret
metadata:
  name: ${delegateName}-proxy
  namespace: harness-delegate-ng
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
    harness.io/name: ${delegateName}
  name: ${delegateName}
  namespace: harness-delegate-ng
spec:
  replicas: ${delegateReplicas}
  podManagementPolicy: Parallel
  selector:
    matchLabels:
      harness.io/name: ${delegateName}
  serviceName: ""
  template:
    metadata:
      labels:
        harness.io/name: ${delegateName}
    spec:
      containers:
      - image: ${delegateDockerImage}
        imagePullPolicy: Always
        name: harness-delegate-instance
        <#if ciEnabled == "true">
        ports:
          - containerPort: ${delegateGrpcServicePort}
        </#if>
        resources:
          limits:
            cpu: "${delegateCpu}"
            memory: "${delegateRam}Mi"
          requests:
            cpu: "${delegateRequestsCpu}"
            memory: "${delegateRequestsRam}Mi"
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
        - name: NEXT_GEN
          value: "true"
        - name: DELEGATE_DESCRIPTION
          value: "${delegateDescription}"
        - name: DELEGATE_PROFILE
          value: "${delegateProfile}"
        - name: DELEGATE_TYPE
          value: "${delegateType}"
        - name: DELEGATE_TAGS
          value: "${delegateTags}"
        - name: DELEGATE_TASK_LIMIT
          value: "${delegateTaskLimit}"
        - name: DELEGATE_ORG_IDENTIFIER
          value: "${delegateOrgIdentifier}"
        - name: DELEGATE_PROJECT_IDENTIFIER
          value: "${delegateProjectIdentifier}"
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
        - name: INIT_SCRIPT
          value: ""
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
        - name: GRPC_SERVICE_ENABLED
          value: "${grpcServiceEnabled}"
        - name: GRPC_SERVICE_CONNECTOR_PORT
          value: "${grpcServiceConnectorPort}"
        - name: VERSION_CHECK_DISABLED
          value: "${versionCheckDisabled}"
        - name: DELEGATE_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
      restartPolicy: Always

<#if ciEnabled == "true">
---

apiVersion: v1
kind: Service
metadata:
  name: delegate-service
  namespace: harness-delegate-ng
spec:
  type: ClusterIP
  selector:
    harness.io/app: harness-delegate
    harness.io/account: ${kubernetesAccountLabel}
    harness.io/name: ${delegateName}
  ports:
    - port: ${delegateGrpcServicePort}
</#if>