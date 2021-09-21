package software.wings.beans.cloudformation;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudFormationCommandTaskParameters implements TaskParameters {
  private CloudFormationCommandRequest cloudFormationCommandRequest;
  private List<EncryptedDataDetail> encryptedDataDetails;
}
