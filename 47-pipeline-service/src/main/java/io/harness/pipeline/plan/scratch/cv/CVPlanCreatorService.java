package io.harness.pipeline.plan.scratch.cv;

import io.harness.pipeline.plan.scratch.common.creator.PlanCreatorService;
import io.harness.serializer.KryoSerializer;

public class CVPlanCreatorService extends PlanCreatorService {
  public CVPlanCreatorService(KryoSerializer kryoSerializer) {
    super(kryoSerializer, new CVPlanCreatorProvider());
  }
}
