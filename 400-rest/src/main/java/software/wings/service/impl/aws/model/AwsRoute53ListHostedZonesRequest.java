package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.service.impl.aws.model.AwsRoute53Request.AwsRoute53RequestType.LIST_HOSTED_ZONES;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class AwsRoute53ListHostedZonesRequest extends AwsRoute53Request {
  private String region;

  @Builder
  public AwsRoute53ListHostedZonesRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    super(awsConfig, encryptionDetails, LIST_HOSTED_ZONES);
    this.region = region;
  }
}
