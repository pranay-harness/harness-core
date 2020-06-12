package io.harness.cdng.pipeline;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.yaml.core.intfc.Stage;

@JsonDeserialize()
public interface CDStage extends Stage {}
