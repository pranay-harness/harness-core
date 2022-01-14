package io.harness.pms.sdk.core.adviser.success;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@TypeAlias("onSuccessAdviserParameters")
public class OnSuccessAdviserParameters {
  String nextNodeId;
}
