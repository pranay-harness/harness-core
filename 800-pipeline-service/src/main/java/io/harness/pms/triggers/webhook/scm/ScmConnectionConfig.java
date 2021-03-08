package io.harness.pms.triggers.webhook.scm;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScmConnectionConfig {
  String url;
}
