package io.harness.cdng.creator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.executions.steps.StepSpecTypeConstants;

import com.google.common.collect.Sets;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class CDCreatorUtils {
  public Set<String> getSupportedSteps() {
    return Sets.newHashSet("K8sRollingDeploy", "K8sRollingRollback", "K8sScale", "K8sCanaryDeploy",
        "K8sBlueGreenDeploy", "K8sBGSwapServices", "K8sDelete", "K8sCanaryDelete", "K8sApply", "TerraformApply",
        "TerraformPlan", "TerraformDestroy", StepSpecTypeConstants.TERRAFORM_ROLLBACK);
  }
}
