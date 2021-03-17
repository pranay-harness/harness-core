package software.wings.service.mappers.artifact;

import io.harness.aws.beans.AwsInternalConfig;

import software.wings.beans.AwsConfig;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AwsConfigToInternalMapper {
  public AwsInternalConfig toAwsInternalConfig(AwsConfig awsConfig) {
    return AwsInternalConfig.builder()
        .accessKey(awsConfig.getAccessKey())
        .secretKey(awsConfig.getSecretKey())
        .useEc2IamCredentials(awsConfig.isUseEc2IamCredentials())
        .crossAccountAttributes(awsConfig.getCrossAccountAttributes())
        .defaultRegion(awsConfig.getDefaultRegion())
        .useIRSA(awsConfig.isUseIRSA())
        .build();
  }
}
