package io.harness.pipeline.plan.scratch.lib.creator;

public abstract class SequentialChildrenPlanCreator extends ChildrenPlanCreator {
  @Override
  public boolean isParallel() {
    return false;
  }
}
