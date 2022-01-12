/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.handler.segment;

import io.harness.secret.ConfigSecret;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author rktummala on 05/08/19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class SegmentConfig {
  @JsonProperty(defaultValue = "false") private boolean enabled;
  private String url;
  @ConfigSecret private String apiKey;
}
