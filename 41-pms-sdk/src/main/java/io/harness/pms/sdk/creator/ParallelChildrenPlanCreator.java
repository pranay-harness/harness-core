package io.harness.pms.sdk.creator;

public abstract class ParallelChildrenPlanCreator extends ChildrenPlanCreator {
  @Override
  public boolean isParallel() {
    return true;
  }
}
