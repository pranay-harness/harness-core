package io.harness.pipeline.plan.scratch.cd;

import io.harness.pipeline.plan.scratch.common.creator.PlanCreatorService;

public class CDPlanCreatorService extends PlanCreatorService {
  public CDPlanCreatorService() {
    super(new CDPlanCreatorProvider());
  }
}
