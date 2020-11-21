package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsAmiServiceTrafficShiftAlbSetupResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private String newAsgName;
  private String lastDeployedAsgName;
  private Integer harnessRevision;
  private List<String> oldAsgNames;
  private AwsAmiPreDeploymentData preDeploymentData;
  private int minInstances;
  private int maxInstances;
  private int desiredInstances;
  private List<String> baseAsgScalingPolicyJSONs;
  private String baseLaunchTemplateName;
  private String baseLaunchTemplateVersion;
  private String newLaunchTemplateName;
  private String newLaunchTemplateVersion;
  private List<LbDetailsForAlbTrafficShift> lbDetailsWithTargetGroups;
}
