package software.wings.service.impl.aws.model;

import io.harness.annotations.dev.Module;
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
public class AwsElbListTargetGroupsRequest extends AwsElbRequest {
  private String loadBalancerName;

  @Builder
  public AwsElbListTargetGroupsRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String loadBalancerName) {
    super(awsConfig, encryptionDetails, AwsElbRequestType.LIST_TARGET_GROUPS_FOR_ALBS, region);
    this.loadBalancerName = loadBalancerName;
  }
}
