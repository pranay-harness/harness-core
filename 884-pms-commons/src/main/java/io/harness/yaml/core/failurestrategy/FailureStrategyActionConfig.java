package io.harness.yaml.core.failurestrategy;

import static io.harness.beans.rollback.NGFailureActionTypeConstants.ABORT;
import static io.harness.beans.rollback.NGFailureActionTypeConstants.IGNORE;
import static io.harness.beans.rollback.NGFailureActionTypeConstants.MANUAL_INTERVENTION;
import static io.harness.beans.rollback.NGFailureActionTypeConstants.MARK_AS_SUCCESS;
import static io.harness.beans.rollback.NGFailureActionTypeConstants.RETRY;
import static io.harness.beans.rollback.NGFailureActionTypeConstants.STAGE_ROLLBACK;
import static io.harness.beans.rollback.NGFailureActionTypeConstants.STEP_GROUP_ROLLBACK;

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.core.failurestrategy.abort.AbortFailureActionConfig;
import io.harness.yaml.core.failurestrategy.ignore.IgnoreFailureActionConfig;
import io.harness.yaml.core.failurestrategy.manualintervention.ManualInterventionFailureActionConfig;
import io.harness.yaml.core.failurestrategy.marksuccess.MarkAsSuccessFailureActionConfig;
import io.harness.yaml.core.failurestrategy.retry.RetryFailureActionConfig;
import io.harness.yaml.core.failurestrategy.rollback.StageRollbackFailureActionConfig;
import io.harness.yaml.core.failurestrategy.rollback.StepGroupFailureActionConfig;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;

@JsonTypeInfo(use = NAME, property = "type", include = PROPERTY, visible = true)
@JsonSubTypes({
  @Type(value = AbortFailureActionConfig.class, name = ABORT)
  , @Type(value = IgnoreFailureActionConfig.class, name = IGNORE),
      @Type(value = ManualInterventionFailureActionConfig.class, name = MANUAL_INTERVENTION),
      @Type(value = MarkAsSuccessFailureActionConfig.class, name = MARK_AS_SUCCESS),
      @Type(value = RetryFailureActionConfig.class, name = RETRY),
      @Type(value = StageRollbackFailureActionConfig.class, name = STAGE_ROLLBACK),
      @Type(value = StepGroupFailureActionConfig.class, name = STEP_GROUP_ROLLBACK)
})
@OwnedBy(HarnessTeam.PIPELINE)
// TODO this should go to yaml commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
public interface FailureStrategyActionConfig {
  @NotNull NGFailureActionType getType();
}
