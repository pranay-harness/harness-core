package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class AwsAmiServiceSetupResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private String newAsgName;
  private String lastDeployedAsgName;
  private Integer harnessRevision;
  private List<String> oldAsgNames;
  private AwsAmiPreDeploymentData preDeploymentData;
  private boolean blueGreen;
  private int minInstances;
  private int maxInstances;
  private int desiredInstances;
  private List<String> baseAsgScalingPolicyJSONs;
  private String baseLaunchTemplateName;
  private String baseLaunchTemplateVersion;
  private String newLaunchTemplateName;
  private String newLaunchTemplateVersion;
}
