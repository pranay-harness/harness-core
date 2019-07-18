package software.wings.graphql.schema.type;

/**
 * @author rktummala on 07/18/19
 */
public enum QLConnectorType implements QLEnum {
  JIRA,
  SLACK,
  SMTP,
  SERVICENOW,
  DOCKER;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
