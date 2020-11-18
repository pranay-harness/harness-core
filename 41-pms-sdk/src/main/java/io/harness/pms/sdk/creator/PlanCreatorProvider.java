package io.harness.pms.sdk.creator;

import java.util.List;

public interface PlanCreatorProvider {
  String getServiceName();
  List<PartialPlanCreator<?>> getPlanCreators();
}
