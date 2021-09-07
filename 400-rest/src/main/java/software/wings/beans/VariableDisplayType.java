package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._957_CG_BEANS)
@OwnedBy(CDC)
public enum VariableDisplayType {
  TEXT("Text"),
  SERVICE("Service"),
  ENVIRONMENT("Environment"),
  INFRASTRUCTURE_DEFINITION("Infrastructure definition"),
  APPDYNAMICS_CONFIGID("AppDynamics Server"),
  APPDYNAMICS_APPID("AppDynamics Application"),
  APPDYNAMICS_TIERID("AppDynamics Tier"),
  ELK_INDICES("Elastic Search Index"),
  ELK_CONFIGID("Elastic Search Server"),
  NEWRELIC_CONFIGID("New Relic Server"),
  NEWRELIC_APPID("New Relic Application"),
  NEWRELIC_MARKER_CONFIGID("New Relic Marker Server"),
  NEWRELIC_MARKER_APPID("New Relic Marker Application"),
  SUMOLOGIC_CONFIGID("Sumo Logic Server"),
  SPLUNK_CONFIGID("Splunk Server"),
  CF_AWS_CONFIG_ID("AWS Cloud Provider"),
  SS_SSH_CONNECTION_ATTRIBUTE("SSH Connection Attribute"),
  SS_WINRM_CONNECTION_ATTRIBUTE("WinRM Connection Attribute"),
  USER_GROUP("User Group"),
  HELM_GIT_CONFIG_ID("Helm Git Connector Id"),
  JENKINS_SERVER("Jenkins Server"),
  GCP_CONFIG("Google Cloud Provider"),
  GIT_CONFIG("Source Repository");

  public String getDisplayName() {
    return displayName;
  }

  private String displayName;

  VariableDisplayType(String displayName) {
    this.displayName = displayName;
  }
}
