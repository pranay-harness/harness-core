package io.harness.app;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScmConnectionConfig {
  String url;
}
