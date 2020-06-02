package io.harness.cdng.pipeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.yaml.core.intfc.StepInfo;

@JsonIgnoreProperties
public interface CDStepInfo extends StepInfo {}
