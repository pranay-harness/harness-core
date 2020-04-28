package software.wings.beans;

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
  HELM_GIT_CONFIG_ID("Helm Git Connector Id");

  public String getDisplayName() {
    return displayName;
  }

  private String displayName;

  VariableDisplayType(String displayName) {
    this.displayName = displayName;
  }
}