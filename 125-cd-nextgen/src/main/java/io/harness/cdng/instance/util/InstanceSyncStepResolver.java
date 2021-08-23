package io.harness.cdng.instance.util;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.util.Objects.nonNull;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.k8s.K8sBlueGreenStep;
import io.harness.cdng.k8s.K8sCanaryStep;
import io.harness.cdng.k8s.K8sRollingRollbackStep;
import io.harness.cdng.k8s.K8sRollingStep;
import io.harness.pms.contracts.steps.StepType;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public class InstanceSyncStepResolver {
  public final Set<String> INSTANCE_SYN_STEP_TYPES =
      Collections.unmodifiableSet(Sets.newHashSet(K8sRollingStep.STEP_TYPE.getType(), K8sCanaryStep.STEP_TYPE.getType(),
          K8sBlueGreenStep.STEP_TYPE.getType(), K8sRollingRollbackStep.STEP_TYPE.getType()));

  public boolean shouldRunInstanceSync(StepType stepType) {
    return nonNull(stepType) && INSTANCE_SYN_STEP_TYPES.contains(stepType.getType());
  }
}
