package io.harness.beans.stages;

import io.harness.yaml.core.intfc.StageType;
import io.harness.yaml.core.intfc.WithTypeEnum;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize()
public interface CIStage extends StageType, WithTypeEnum<CIStageType> {}
