package io.harness.ci.plan.creator.step;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.plan.creator.CICreatorUtils;
import io.harness.plancreator.steps.GenericStepPMSPlanCreator;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import java.util.Set;

@OwnedBy(HarnessTeam.CI)
public class CIPMSStepPlanCreator extends GenericStepPMSPlanCreator {
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Set<String> getSupportedStepTypes() {
    return CICreatorUtils.getSupportedSteps();
  }
}