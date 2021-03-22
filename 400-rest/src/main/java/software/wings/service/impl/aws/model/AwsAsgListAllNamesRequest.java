package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.service.impl.aws.model.AwsAsgRequest.AwsAsgRequestType.LIST_ALL_ASG_NAMES;

import io.harness.annotations.dev.HarnessModule;
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
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class AwsAsgListAllNamesRequest extends AwsAsgRequest {
  @Builder
  public AwsAsgListAllNamesRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    super(awsConfig, encryptionDetails, LIST_ALL_ASG_NAMES, region);
  }
}
