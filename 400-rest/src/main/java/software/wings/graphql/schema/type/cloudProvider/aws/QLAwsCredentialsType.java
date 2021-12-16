package software.wings.graphql.schema.type.cloudProvider.aws;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLAwsCredentialsType implements QLEnum {
  EC2_IAM,
  MANUAL,
  IRSA;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
