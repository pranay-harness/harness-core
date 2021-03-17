package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsAsgRequest.AwsAsgRequestType.LIST_ASG_INSTANCES;

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
public class AwsAsgListInstancesRequest extends AwsAsgRequest {
  private String autoScalingGroupName;

  @Builder
  public AwsAsgListInstancesRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    super(awsConfig, encryptionDetails, LIST_ASG_INSTANCES, region);
    this.autoScalingGroupName = autoScalingGroupName;
  }
}
