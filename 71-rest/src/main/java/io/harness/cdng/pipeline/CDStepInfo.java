package io.harness.cdng.pipeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.executionplan.plancreator.beans.GenericStepInfo;
import io.harness.yaml.core.intfc.StepInfo;

@JsonIgnoreProperties

@JsonDeserialize
public interface CDStepInfo extends StepInfo, GenericStepInfo {}
