package io.harness.pms.execution.facilitator;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@TypeAlias("defaultFacilitatorParams")
public class DefaultFacilitatorParams {
  @Builder.Default Duration waitDurationSeconds = Duration.ofSeconds(0);
}
