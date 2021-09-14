/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.secretmanagerclient.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VaultSecretEngineDTO {
  private String name;
  private String description;
  private String type;
  private Integer version;
}
