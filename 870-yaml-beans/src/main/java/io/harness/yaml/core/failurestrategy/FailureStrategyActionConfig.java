package io.harness.yaml.core.failurestrategy;

import static io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants.ABORT;
import static io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants.IGNORE;
import static io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants.MANUAL_INTERVENTION;
import static io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants.MARK_AS_SUCCESS;
import static io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants.RETRY;
import static io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants.STAGE_ROLLBACK;
import static io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants.STEP_GROUP_ROLLBACK;

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.yaml.core.failurestrategy.abort.AbortFailureActionConfig;
import io.harness.yaml.core.failurestrategy.ignore.IgnoreFailureActionConfig;
import io.harness.yaml.core.failurestrategy.manualintervention.ManualInterventionFailureActionConfig;
import io.harness.yaml.core.failurestrategy.marksuccess.MarkAsSuccessFailureActionConfig;
import io.harness.yaml.core.failurestrategy.retry.RetryFailureActionConfig;
import io.harness.yaml.core.failurestrategy.rollback.StageRollbackFailureActionConfig;
import io.harness.yaml.core.failurestrategy.rollback.StepGroupFailureActionConfig;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, property = "type", include = PROPERTY, visible = true)
@JsonSubTypes({
  @Type(value = AbortFailureActionConfig.class, name = ABORT)
  , @Type(value = IgnoreFailureActionConfig.class, name = IGNORE),
      @Type(value = ManualInterventionFailureActionConfig.class, name = MANUAL_INTERVENTION),
      @Type(value = MarkAsSuccessFailureActionConfig.class, name = MARK_AS_SUCCESS),
      @Type(value = RetryFailureActionConfig.class, name = RETRY),
      @Type(value = StageRollbackFailureActionConfig.class, name = STAGE_ROLLBACK),
      @Type(value = StepGroupFailureActionConfig.class, name = STEP_GROUP_ROLLBACK),
})
public interface FailureStrategyActionConfig {
  @JsonIgnore NGFailureActionType getType();
}
