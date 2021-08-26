package io.harness.cdng.advisers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("rollbackCustomStepParameters")
@OwnedBy(CDC)
@RecasterAlias("io.harness.cdng.advisers.RollbackCustomStepParameters")
public class RollbackCustomStepParameters implements StepParameters {}
