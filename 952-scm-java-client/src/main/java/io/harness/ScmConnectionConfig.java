package io.harness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@OwnedBy(DX)
public class ScmConnectionConfig {
  String url;
}
