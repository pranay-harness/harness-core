package io.harness.cdng.pipeline;

import io.harness.yaml.core.intfc.StageType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize()
public interface CDStage extends StageType {}
