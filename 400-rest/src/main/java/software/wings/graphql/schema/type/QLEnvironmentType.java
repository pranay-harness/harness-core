package software.wings.graphql.schema.type;

public enum QLEnvironmentType implements QLEnum {
  PROD,
  NON_PROD;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
