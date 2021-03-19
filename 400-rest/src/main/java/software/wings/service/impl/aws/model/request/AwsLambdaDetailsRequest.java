package software.wings.service.impl.aws.model.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsLambdaRequest;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class AwsLambdaDetailsRequest extends AwsLambdaRequest {
  private String functionName;
  private String qualifier;
  private Boolean loadAliases;

  @Builder
  public AwsLambdaDetailsRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String functionName, String qualifier, Boolean loadAliases) {
    super(awsConfig, encryptionDetails, AwsLambdaRequestType.LAMBDA_FUNCTION_DETAILS, region);
    this.functionName = functionName;
    this.qualifier = qualifier;
    this.loadAliases = loadAliases;
  }
}
