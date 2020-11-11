package io.harness.pms.sdk.creator;

public abstract class SequentialChildrenPlanCreator extends ChildrenPlanCreator {
  @Override
  public boolean isParallel() {
    return false;
  }
}
