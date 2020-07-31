package io.harness.cdng.pipeline;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.yaml.core.intfc.StageType;

@JsonDeserialize()
public interface CDStage extends StageType {}
