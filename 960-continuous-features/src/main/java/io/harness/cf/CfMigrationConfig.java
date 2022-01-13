/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cf;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.secret.ConfigSecret;

import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.CF)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class CfMigrationConfig {
  private boolean enabled;
  private String adminUrl;
  @ConfigSecret private String apiKey;
  private String account;
  private String org;
  private String project;
  private String environment;
  @Default private int connectionTimeout = 10000;
  @Default private int readTimeout = 10000;
}
