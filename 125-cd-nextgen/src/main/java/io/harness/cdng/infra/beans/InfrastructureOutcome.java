package io.harness.cdng.infra.beans;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.steps.environment.EnvironmentOutcome;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@OwnedBy(HarnessTeam.CDP)
@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
public interface InfrastructureOutcome extends Outcome, PassThroughData {
  String getKind();
  EnvironmentOutcome getEnvironment();
  String getInfrastructureKey();
}
