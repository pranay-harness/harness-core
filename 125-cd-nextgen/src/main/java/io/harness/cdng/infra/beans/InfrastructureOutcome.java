package io.harness.cdng.infra.beans;

import io.harness.data.Outcome;
import io.harness.facilitator.PassThroughData;

public interface InfrastructureOutcome extends Outcome, PassThroughData { String getKind(); }
