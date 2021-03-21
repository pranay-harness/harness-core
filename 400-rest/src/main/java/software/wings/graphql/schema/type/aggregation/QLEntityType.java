package software.wings.graphql.schema.type.aggregation;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
