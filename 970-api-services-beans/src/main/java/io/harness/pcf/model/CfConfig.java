/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pcf.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@OwnedBy(CDP)
public class CfConfig {
  @NotEmpty private String endpointUrl;
  private char[] username;
  private char[] password;
}
