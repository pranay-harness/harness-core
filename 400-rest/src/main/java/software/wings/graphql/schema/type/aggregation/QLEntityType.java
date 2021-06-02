package software.wings.graphql.schema.type.aggregation;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(PL)
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
  COLLABORATION_PROVIDER,
  PROVISIONER;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
