package io.harness.pipeline.plan.scratch.cv;

import io.harness.pipeline.plan.scratch.common.creator.PlanCreatorService;

public class CVPlanCreatorService extends PlanCreatorService {
  public CVPlanCreatorService() {
    super(new CVPlanCreatorProvider());
  }
}
