apiVersion: v1
kind: ConfigMap
metadata:
  name: harness-autostopping-config
data:
  account_id: ${accountId}
  connector_id: ${connectorIdentifier}

---
apiVersion: apiextensions.k8s.io/v1
kind: CustomResourceDefinition
metadata:
  annotations:
    controller-gen.kubebuilder.io/version: v0.4.1
  creationTimestamp: null
  name: autostoppingrules.lightwing.lightwing.io
spec:
  group: lightwing.lightwing.io
  names:
    kind: AutoStoppingRule
    listKind: AutoStoppingRuleList
    plural: autostoppingrules
    singular: autostoppingrule
  scope: Namespaced
  versions:
  - name: v1
    schema:
      openAPIV3Schema:
        x-kubernetes-preserve-unknown-fields: true
        description: AutoStoppingRule is the Schema for the autostoppingrules API
        properties:
          apiVersion:
            description: 'APIVersion defines the versioned schema of this representation
              of an object. Servers should convert recognized schemas to the latest
              internal value, and may reject unrecognized values. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#resources'
            type: string
          kind:
            description: 'Kind is a string value representing the REST resource this
              object represents. Servers may infer this from the endpoint the client
              submits requests to. Cannot be updated. In CamelCase. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#types-kinds'
            type: string
          metadata:
            type: object
          spec:
            x-kubernetes-preserve-unknown-fields: true
            description: AutoStoppingRuleSpec defines the desired state of AutoStoppingRule
            properties:
              foo:
                description: Foo is an example field of AutoStoppingRule. Edit autostoppingrule_types.go
                  to remove/update
                type: string
            type: object
          status:
            x-kubernetes-preserve-unknown-fields: true
            description: AutoStoppingRuleStatus defines the observed state of AutoStoppingRule
            type: object
        type: object
    served: true
    storage: true
    subresources:
      status: {}
status:
  acceptedNames:
    kind: ""
    plural: ""
  conditions: []
  storedVersions: []

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: as-controller-config
data:
  envoy.yaml: >
    admin:
      profile_path: /tmp/envoy.prof
      access_log_path: /tmp/envoy_admin.log
      address:
        socket_address: { address: 0.0.0.0, port_value: 9901 }
    node:
      cluster: test-cluster
      id: test-id

    dynamic_resources:
      lds_config:
        resource_api_version: V3
        api_config_source:
          api_type: GRPC
          transport_api_version: V3
          grpc_services:
            - envoy_grpc:
                cluster_name: xds_cluster
      cds_config:
        resource_api_version: V3
        api_config_source:
          api_type: GRPC
          transport_api_version: V3
          grpc_services:
            - envoy_grpc:
                cluster_name: xds_cluster

    static_resources:
      clusters:
      - name: xds_cluster
        connect_timeout: 0.25s
        type: STRICT_DNS
        lb_policy: ROUND_ROBIN
        typed_extension_protocol_options:
          envoy.extensions.upstreams.http.v3.HttpProtocolOptions:
            "@type": type.googleapis.com/envoy.extensions.upstreams.http.v3.HttpProtocolOptions
            explicit_http_config:
              http2_protocol_options:
                connection_keepalive:
                  interval: 30s
                  timeout: 5s
        load_assignment:
          cluster_name: xds_cluster
          endpoints:
          - lb_endpoints:
            - endpoint:
                address:
                  socket_address:
                    address: harness-operator.harness-autostopping.svc.cluster.local
                    port_value: 18000
      - name: harness_api_endpoint
        connect_timeout: 0.25s
        type: LOGICAL_DNS
        lb_policy: ROUND_ROBIN
        load_assignment:
          cluster_name: harness_api_endpoint
          endpoints:
          - lb_endpoints:
            - endpoint:
                address:
                  socket_address:
                    address: ${envoyHarnessHostname}
                    port_value: 80
---
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: ascontroller
  name: ascontroller
  namespace: default
spec:
  replicas: 1
  revisionHistoryLimit: 10
  selector:
    matchLabels:
      app: ascontroller
  template:
    metadata:
      labels:
        app: ascontroller
    spec:
      containers:
      - args:
        - -c
        - /etc/envoy.yaml
        command:
        - envoy
        image: envoyproxy/envoy:v1.18-latest
        imagePullPolicy: Always
        name: envoy
        ports:
        - containerPort: 10000
          protocol: TCP
          name: listener
        - containerPort: 9901
          protocol: TCP
          name: admin
        resources: {}
        volumeMounts:
        - mountPath: /etc/envoy.yaml
          name: as-controller-config
          subPath: envoy.yaml
      dnsPolicy: ClusterFirst
      restartPolicy: Always
      volumes:
      - configMap:
          defaultMode: 420
          name: as-controller-config
        name: as-controller-config

---

apiVersion: v1
kind: Service
metadata:
  name: ascontroller
  namespace: default
spec:
  ports:
  - port: 80
    protocol: TCP
    targetPort: 10000
  selector:
    app: ascontroller
  type: ClusterIP
---
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: harness-operator
  name: harness-operator
  namespace: harness-autostopping
spec:
  selector:
    matchLabels:
      app: harness-operator
  replicas: 1
  template:
    metadata:
      labels:
        app: harness-operator
    spec:
      containers:
      - name: harness-operator
        image: registry.gitlab.com/lightwing/lightwing/operator:latest
        imagePullPolicy: Always
        env:
        - name: HARNESS_API
          value: "${harnessHostname}/gateway/lw/api"
        - name: CONNECTOR_ID
          value: ${connectorIdentifier}
        - name: REMOTE_ACCOUNT_ID
          value: ${accountId}
        ports:
        - containerPort: 18000
      imagePullSecrets:
      - name: gitlab-auth
      serviceAccountName: harness-autostopping-sa
---
apiVersion: v1
kind: Service
metadata:
  name: harness-operator
  namespace: harness-autostopping
  labels:
    app: harness-operator
spec:
  ports:
  - port: 18000
    protocol: TCP
  selector:
    app: harness-operator
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: harness-autostopping-sa
  namespace: harness-autostopping
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: harness-autostopping-sa
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-admin
subjects:
  - kind: ServiceAccount
    name: harness-autostopping-sa
    namespace: harness-autostopping
---
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: harness-progress
  name: harness-progress
  namespace: harness-autostopping
spec:
  selector:
    matchLabels:
      app: harness-progress
  replicas: 1
  template:
    metadata:
      labels:
        app: harness-progress
    spec:
      containers:
      - name: harness-progress
        image: registry.gitlab.com/lightwing/lightwing/httpproxy:latest
        imagePullPolicy: Always
        env:
        - name: HARNESS_API_URL
          value: "${harnessHostname}/gateway/lw/api/"
        ports:
        - containerPort: 8093
      imagePullSecrets:
      - name: gitlab-auth
---
apiVersion: v1
kind: Service
metadata:
  name: harness-progress
  namespace: harness-autostopping
  labels:
    app: harness-progress
spec:
  ports:
  - port: 80
    protocol: TCP
    targetPort: 8093
  selector:
    app: harness-progress