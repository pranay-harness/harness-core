package io.harness.monitoring;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FailedTestData {
  private String author;
  private String team;
  private int numFailedTests;
}
