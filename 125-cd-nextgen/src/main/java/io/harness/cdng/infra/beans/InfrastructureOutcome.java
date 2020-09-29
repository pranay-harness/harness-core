package io.harness.cdng.infra.beans;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.data.Outcome;
import io.harness.facilitator.PassThroughData;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
public interface InfrastructureOutcome extends Outcome, PassThroughData {
  String getKind();
}
