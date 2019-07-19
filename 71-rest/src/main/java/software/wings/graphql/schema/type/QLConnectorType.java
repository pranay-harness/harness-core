package software.wings.graphql.schema.type;

/**
 * @author rktummala on 07/18/19
 */
public enum QLConnectorType implements QLEnum {
  SMTP,
  JENKINS,
  BAMBOO,
  SPLUNK,
  ELK,
  LOGZ,
  SUMO,
  APP_DYNAMICS,
  NEW_RELIC,
  DYNA_TRACE,
  BUG_SNAG,
  DATA_DOG,
  APM_VERIFICATION,
  PROMETHEUS,
  ELB,
  SLACK,
  DOCKER,
  ECR,
  GCR,
  NEXUS,
  ARTIFACTORY,
  AMAZON_S3,
  GCS,
  GIT,
  SMB,
  JIRA,
  SFTP,
  SERVICENOW;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
