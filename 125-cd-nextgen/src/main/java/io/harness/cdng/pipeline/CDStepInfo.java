package io.harness.cdng.pipeline;

import io.harness.executionplan.plancreator.beans.GenericStepInfo;
import io.harness.yaml.core.StepSpecType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize
public interface CDStepInfo extends GenericStepInfo, StepSpecType {}
