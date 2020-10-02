package io.harness.rule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LeaderInfo {
  String user;
  String team;
  String email;
}
