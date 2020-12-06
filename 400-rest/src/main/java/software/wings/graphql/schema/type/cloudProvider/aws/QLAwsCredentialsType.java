package software.wings.graphql.schema.type.cloudProvider.aws;

import software.wings.graphql.schema.type.QLEnum;

public enum QLAwsCredentialsType implements QLEnum {
  EC2_IAM,
  MANUAL;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
