package io.harness.pipeline.plan.scratch.lib.creator;

public abstract class ParallelChildrenPlanCreator extends ChildrenPlanCreator {
  @Override
  public boolean isParallel() {
    return true;
  }
}
