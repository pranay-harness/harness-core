package io.harness.pms.triggers.scm;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScmConnectionConfig {
  String url;
}
