package io.harness.plancreator.steps.http;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.AbstractStepNode;
import io.harness.steps.StepSpecTypeConstants;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;

@OwnedBy(PIPELINE)
@ApiModel(subTypes = {HttpStepInfo.class})
@JsonSubTypes({ @JsonSubTypes.Type(value = HttpStepInfo.class, name = StepSpecTypeConstants.HTTP) })
public abstract class PmsAbstractStepNode extends AbstractStepNode {}
