<#macro ng>
apiVersion: v1
kind: Service
metadata:
  name: delegate-service
  namespace: ${delegateNamespace}
spec:
  type: ClusterIP
  selector:
    harness.io/name: ${delegateName}
  ports:
    - port: ${delegateGrpcServicePort}
</#macro>
<#macro cg>
apiVersion: v1
kind: Service
metadata:
  name: delegate-service
  namespace: harness-delegate
spec:
  type: ClusterIP
  selector:
    harness.io/app: harness-delegate
    harness.io/account: ${kubernetesAccountLabel}
    harness.io/name: ${delegateName}
  ports:
    - port: ${delegateGrpcServicePort}
</#macro>
