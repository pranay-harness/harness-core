package io.harness.yaml.core.failurestrategy;

public interface NGFailureActionTypeConstants {
  String IGNORE = "Ignore";
  String RETRY = "Retry";
  String ABORT = "Abort";
  String STAGE_ROLLBACK = "StageRollback";
  String STEP_GROUP_ROLLBACK = "StepGroupRollback";
  String MANUAL_INTERVENTION = "ManualIntervention";
  String MARK_AS_SUCCESS = "MarkAsSuccess";
}
