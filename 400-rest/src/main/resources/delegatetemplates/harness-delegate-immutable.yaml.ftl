<#import "common/delegate-environment.ftl" as delegateEnvironment>
<#import "common/delegate-role.ftl" as delegateRole>
<#import "common/delegate-service.ftl" as delegateService>
<#import "common/upgrader.ftl" as upgrader>
apiVersion: v1
kind: Namespace
metadata:
  name: ${delegateNamespace}

---

<#if enableCE == "true">
    <@delegateRole.cgCe />
<#else>
    <@delegateRole.cg />
</#if>

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
    harness.io/app: harness-delegate
    harness.io/account: ${kubernetesAccountLabel}
    harness.io/name: ${delegateName}
  # Name must contain the six letter account identifier: ${kubernetesAccountLabel}
  name: ${delegateName}-${kubernetesAccountLabel}
  namespace: ${delegateNamespace}
spec:
  revisionHistoryLimit: 3
  replicas: 1
  selector:
    matchLabels:
      harness.io/app: ${delegateNamespace}
      harness.io/account: ${kubernetesAccountLabel}
      harness.io/name: ${delegateName}
  template:
    metadata:
      labels:
        harness.io/app: ${delegateNamespace}
        harness.io/account: ${kubernetesAccountLabel}
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
            memory: "${delegateRam}Gi"
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
<@delegateEnvironment.cgSpecific />
<@delegateEnvironment.immutable />
      restartPolicy: Always

<#if ciEnabled == "true">
---

    <@delegateService.cg />
</#if>

---

<@upgrader.cronjob />
