package io.harness.app.schema.query.delegate;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import io.harness.app.schema.type.delegate.QLDelegateStatus;
import io.harness.app.schema.type.delegate.QLDelegateType;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.QLEnum;
import software.wings.graphql.schema.type.aggregation.EntityFilter;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(DEL)
public class QLDelegateFilter implements EntityFilter {
  String accountId;
  QLDelegateType delegateType;
  QLDelegateStatus delegateStatus;
  String delegateName;

    public enum QLTaskGroup implements QLEnum {
      SCRIPT,
      HTTP,
      SPLUNK,
      APPDYNAMICS,
      INSTANA,
      NEWRELIC,
      STACKDRIVER,
      DYNA_TRACE,
      PROMETHEUS,
      CLOUD_WATCH,
      JENKINS,
      COMMAND,
      BAMBOO,
      DOCKER,
      ECR,
      GCR,
      GCS,
      GCB,
      GCP,
      ACR,
      NEXUS,
      S3,
      AZURE_ARTIFACTS,
      AZURE_VMSS,
      AZURE_APP_SERVICE,
      AZURE_ARM,
      ELK,
      LOGZ,
      SUMO,
      ARTIFACTORY,
      HOST_VALIDATION,
      KMS,
      GIT,
      CONTAINER,
      AMI,
      HELM,
      COLLABORATION_PROVIDER,
      PCF,
      SPOTINST,
      APM,
      LOG,
      CLOUD_FORMATION,
      TERRAFORM,
      TERRAGRUNT,
      AWS,
      LDAP,
      K8S,
      SMB,
      SFTP,
      TRIGGER,
      JIRA,
      CONNECTIVITY_VALIDATION,
      BUILD_SOURCE,
      CUSTOM,
      SHELL_SCRIPT_PROVISION,
      SERVICENOW,
      HELM_REPO_CONFIG_VALIDATION,
      HELM_VALUES_FETCH_TASK,
      GUARD_24x7,
      CI,
      SLACK,
      ARTIFACT_COLLECT_NG,
      K8S_NG,
      CAPABILITY_VALIDATION,
      JIRA_NG,
      CVNG,
      NOTIFICATION,
      HTTP_NG,
      SHELL_SCRIPT_NG,
      GIT_NG,
      BATCH_CAPABILITY_CHECK,
      CUSTOM_MANIFEST_VALUES_FETCH_TASK,
      CUSTOM_MANIFEST_FETCH_TASK,
      TERRAFORM_NG,
      CE;

      @Override
      public String getStringValue() {
        return this.name();
      }


    }
}
