package io.harness.rule;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TeamInfo {
  String team;
  @Singular List<String> leaders;
}
