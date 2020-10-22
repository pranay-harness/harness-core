package io.harness.pipeline.plan.scratch.cd;

import io.harness.pipeline.plan.scratch.common.creator.PlanCreatorService;
import io.harness.serializer.KryoSerializer;

public class CDPlanCreatorService extends PlanCreatorService {
  public CDPlanCreatorService(KryoSerializer kryoSerializer) {
    super(kryoSerializer, new CDPlanCreatorProvider());
  }
}
