package io.harness.migration;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Set;

@Value
@Builder
public class MigrationJob {
  private String id;
  private String sha;
  enum Allowance { BACKGROUND }

  @Value
  @Builder
  public static class Metadata {
    @Singular List<MigrationChannel> channels;
    private Set<Allowance> allowances;
  }

  private Metadata metadata;
}
