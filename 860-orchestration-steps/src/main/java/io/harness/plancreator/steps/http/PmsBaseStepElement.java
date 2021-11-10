package io.harness.plancreator.steps.http;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.beans.rollback.NGFailureActionTypeConstants.ABORT;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.BaseStepInfo;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.yaml.core.StepSpecType;
import io.harness.yaml.core.failurestrategy.abort.AbortFailureActionConfig;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;

@OwnedBy(PIPELINE)
@ApiModel(subTypes = {HttpStepInfo.class})
@JsonSubTypes({ @JsonSubTypes.Type(value = HttpStepInfo.class, name = StepSpecTypeConstants.HTTP) })
public abstract class PmsBaseStepElement extends BaseStepInfo {}
