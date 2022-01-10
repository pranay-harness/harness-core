/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.handler.segment;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.secret.ConfigSecret;

import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ujjawal on 12/18/19
 */
@OwnedBy(PL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class SalesforceConfig {
  @ConfigSecret String userName;
  @ConfigSecret String password;
  @ConfigSecret String consumerKey;
  @ConfigSecret String consumerSecret;
  String grantType;
  String loginInstanceDomain;
  String apiVersion;
  boolean enabled;
}
