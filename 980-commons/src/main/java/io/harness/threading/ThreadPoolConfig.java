package io.harness.threading;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class ThreadPoolConfig {
  @Builder.Default int corePoolSize = 1;
  @Builder.Default int maxPoolSize = 5;
  @Builder.Default long idleTime = 30;
  @Builder.Default TimeUnit timeUnit = TimeUnit.SECONDS;
}
