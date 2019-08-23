package io.harness.perpetualtask;

public abstract class AbstractPerpetualTask implements PerpetualTask {
  protected PerpetualTaskId taskId;
  public AbstractPerpetualTask(PerpetualTaskId taskId) {
    this.taskId = taskId;
  }
}
