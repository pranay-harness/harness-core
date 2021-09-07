package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class AwsAmiRequest extends AwsRequest {
  public enum AwsAmiRequestType {
    EXECUTE_AMI_SERVICE_SETUP,
    EXECUTE_AMI_SERVICE_DEPLOY,
    EXECUTE_AMI_SWITCH_ROUTE,
    EXECUTE_AMI_SERVICE_TRAFFIC_SHIFT_ALB_SETUP,
    EXECUTE_AMI_SERVICE_TRAFFIC_SHIFT_ALB_DEPLOY,
    EXECUTE_AMI_SERVICE_TRAFFIC_SHIFT_ALB
  }

  @NotNull private AwsAmiRequestType requestType;
  @NotNull private String region;
  private boolean amiInServiceHealthyStateFFEnabled;
  private boolean amiAsgConfigCopyEnabled;

  public AwsAmiRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, AwsAmiRequestType requestType,
      String region, boolean amiInServiceHealthyStateFFEnabled, boolean amiAsgConfigCopyEnabled) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
    this.region = region;
    this.amiInServiceHealthyStateFFEnabled = amiInServiceHealthyStateFFEnabled;
    this.amiAsgConfigCopyEnabled = amiAsgConfigCopyEnabled;
  }
}
