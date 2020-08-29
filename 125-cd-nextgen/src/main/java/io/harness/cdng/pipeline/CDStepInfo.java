package io.harness.cdng.pipeline;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.executionplan.plancreator.beans.GenericStepInfo;
import io.harness.yaml.core.StepSpecType;

@JsonDeserialize
public interface CDStepInfo extends GenericStepInfo, StepSpecType {}
