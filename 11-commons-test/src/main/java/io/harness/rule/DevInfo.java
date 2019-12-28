package io.harness.rule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DevInfo {
  private String email;
  private String slack;
  private String jira;
  private String team;
}
