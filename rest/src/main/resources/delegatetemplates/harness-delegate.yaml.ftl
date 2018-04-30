apiVersion: apps/v1beta1
kind: StatefulSet
metadata:
  labels:
    harness.io/app: harness-delegate
  name: ${kubernetesDelegateName}
  namespace: harness-delegate
spec:
  replicas: 1
  selector:
    matchLabels:
      harness.io/app: harness-delegate
  serviceName: ""
  template:
    metadata:
      labels:
        harness.io/app: harness-delegate
    spec:
      containers:
      - image: harness/delegate:latest
        imagePullPolicy: Always
        name: harness-delegate-instance
        resources:
          limits:
            cpu: "2"
            memory: 6000Mi
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
        - name: DESCRIPTION
          value: description here
        - name: PROXY_HOST
          value:
        - name: PROXY_PORT
          value:
        - name: PROXY_SCHEME
          value:
        - name: NO_PROXY
          value:
        - name: POLL_FOR_TASKS
          value: false
      restartPolicy: Always
