package software.wings.service.impl.instance;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(PL)
public class Status {
  boolean success;
  String errorMessage;
  boolean retryable;
}
