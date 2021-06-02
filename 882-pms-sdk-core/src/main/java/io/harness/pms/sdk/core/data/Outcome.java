package io.harness.pms.sdk.core.data;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.PipelineViewObject;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@OwnedBy(PIPELINE)
// TODO (P1): Archit Why do we need this here? This needs to be removed
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface Outcome extends StepTransput, PipelineViewObject {}
