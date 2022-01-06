<#import "common/delegate-environment.ftl" as delegateEnvironment>
<#import "common/delegate-role.ftl" as delegateRole>
<#import "common/delegate-service.ftl" as delegateService>
<#import "common/upgrader.ftl" as upgrader>
apiVersion: v1
kind: Namespace
metadata:
  name: ${delegateNamespace}

---

<#switch "${k8sPermissionsType}">
    <#case "CLUSTER_ADMIN">
        <@delegateRole.clusterAdmin />
        <#break>
    <#case "CLUSTER_VIEWER">
        <@delegateRole.clusterViewer />
        <#break>
    <#case "NAMESPACE_ADMIN">
        <@delegateRole.namespaceAdmin />
</#switch>

---

apiVersion: v1
kind: Secret
metadata:
  name: ${delegateName}-proxy
  namespace: ${delegateNamespace}
type: Opaque
data:
  # Enter base64 encoded username and password, if needed
  PROXY_USER: ""
  PROXY_PASSWORD: ""

---

apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    harness.io/name: ${delegateName}
  name: ${delegateName}
  namespace: ${delegateNamespace}
spec:
  revisionHistoryLimit: 3
  replicas: ${delegateReplicas}
  selector:
    matchLabels:
      harness.io/name: ${delegateName}
  template:
    metadata:
      labels:
        harness.io/name: ${delegateName}
    spec:
      terminationGracePeriodSeconds: 600
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
          httpGet:
            path: /api/health
            port: 3460
            scheme: HTTP
          initialDelaySeconds: 20
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /api/health
            port: 3460
            scheme: HTTP
          initialDelaySeconds: 240
          periodSeconds: 10
          failureThreshold: 2
        env:
<@delegateEnvironment.common />
<@delegateEnvironment.ngSpecific />
<@delegateEnvironment.immutable />
      restartPolicy: Always

<#if ciEnabled == "true">
---

    <@delegateService.ng />
</#if>

---

<@upgrader.cronjob />
