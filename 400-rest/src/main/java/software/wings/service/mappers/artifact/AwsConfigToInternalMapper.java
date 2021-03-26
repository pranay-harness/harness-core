package software.wings.service.mappers.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.beans.AwsInternalConfig;

import software.wings.beans.AwsConfig;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CDP)
public class AwsConfigToInternalMapper {
  public AwsInternalConfig toAwsInternalConfig(AwsConfig awsConfig) {
    return AwsInternalConfig.builder()
        .accessKey(awsConfig.getAccessKey())
        .secretKey(awsConfig.getSecretKey())
        .useEc2IamCredentials(awsConfig.isUseEc2IamCredentials())
        .assumeCrossAccountRole(awsConfig.isAssumeCrossAccountRole())
        .crossAccountAttributes(awsConfig.getCrossAccountAttributes())
        .defaultRegion(awsConfig.getDefaultRegion())
        .useIRSA(awsConfig.isUseIRSA())
        .build();
  }
}
