package software.wings.graphql.schema.type.aggregation;

import software.wings.graphql.schema.type.QLEnum;

public enum QLEntityType implements QLEnum {
  APPLICATION,
  SERVICE,
  ENVIRONMENT,
  WORKFLOW,
  PIPELINE,
  INSTANCE,
  DEPLOYMENT,
  CLOUD_PROVIDER,
  CONNECTOR,
  TRIGGER,
  ARTIFACT,
  COLLABORATION_PROVIDER;

  @Override
  public String getStringValue() {
    return this.name();
  }
}