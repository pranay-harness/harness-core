package io.harness.git.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitBaseResult {
  private String accountId;
}
