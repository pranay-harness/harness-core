/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
