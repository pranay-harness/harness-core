package io.harness.rule;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class TeamInfo {
  String team;
  @Singular List<String> leaders;
}
