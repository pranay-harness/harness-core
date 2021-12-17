<#import "upgrader-role.ftl" as upgraderRole>
<#macro cronjob>
<#assign upgraderSaName = "upgrader-cronjob-sa">
<@upgraderRole.cronJobRole upgraderSaName />

---

apiVersion: batch/v1beta1
kind: CronJob
metadata:
  labels:
    harness.io/name: ${delegateName}-upgrader-job
  name: ${delegateName}-upgrader-job
  namespace: ${delegateNamespace}
spec:
  schedule: "*/3 * * * *"
  concurrencyPolicy: Forbid
  startingDeadlineSeconds: 20
  jobTemplate:
    spec:
      template:
        spec:
          serviceAccountName: ${upgraderSaName}
          containers:
            - name: upgrader
              image: ${upgraderDockerImage}
              imagePullPolicy: Always
          restartPolicy: Never
</#macro>