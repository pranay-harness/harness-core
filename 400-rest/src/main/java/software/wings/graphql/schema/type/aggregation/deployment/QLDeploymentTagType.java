package software.wings.graphql.schema.type.aggregation.deployment;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLDeploymentTagType {
  APPLICATION,
  SERVICE,
  ENVIRONMENT,
  DEPLOYMENT
}
