/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migration;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

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
