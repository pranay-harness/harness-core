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
