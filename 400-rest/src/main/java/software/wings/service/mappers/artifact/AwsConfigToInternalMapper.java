package software.wings.service.mappers.artifact;

import io.harness.aws.beans.AwsInternalConfig;
import lombok.experimental.UtilityClass;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsCrossAccountAttributes;

@UtilityClass
public class AwsConfigToInternalMapper {
  public AwsInternalConfig toAwsInternalConfig(String AWS_URL, char[] accessKey, char[] secretKey,
      boolean useEc2IamCredentials, AwsCrossAccountAttributes crossAccountAttributes, String defaultRegion) {
    return AwsInternalConfig.builder()
        .accessKey(accessKey)
        .secretKey(secretKey)
        .useEc2IamCredentials(useEc2IamCredentials)
        .crossAccountAttributes(crossAccountAttributes)
        .defaultRegion(defaultRegion)
        .build();
  }
  public AwsInternalConfig toAwsInternalConfig(AwsConfig awsConfig) {
    return AwsInternalConfig.builder()
        .accessKey(awsConfig.getAccessKey())
        .secretKey(awsConfig.getSecretKey())
        .useEc2IamCredentials(awsConfig.isUseEc2IamCredentials())
        .crossAccountAttributes(awsConfig.getCrossAccountAttributes())
        .defaultRegion(awsConfig.getDefaultRegion())
        .build();
  }
}
