package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsAsgRequest.AwsAsgRequestType.LIST_DESIRED_CAPACITIES;

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
public class AwsAsgListDesiredCapacitiesRequest extends AwsAsgRequest {
  private List<String> asgs;

  @Builder
  public AwsAsgListDesiredCapacitiesRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> asgs) {
    super(awsConfig, encryptionDetails, LIST_DESIRED_CAPACITIES, region);
    this.asgs = asgs;
  }
}
