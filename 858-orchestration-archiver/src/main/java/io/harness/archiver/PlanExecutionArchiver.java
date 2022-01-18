package io.harness.archiver;

public interface PlanExecutionArchiver{
  boolean archive(String planExecutionId);
  boolean restore(String planExecutionId);
}