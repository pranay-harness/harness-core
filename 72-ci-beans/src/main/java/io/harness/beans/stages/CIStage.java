package io.harness.beans.stages;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.yaml.core.intfc.Stage;
import io.harness.yaml.core.intfc.WithTypeEnum;

@JsonDeserialize()
public interface CIStage extends Stage, WithTypeEnum<CIStageType> {}
