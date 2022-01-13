/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.models;

import io.harness.ng.core.dto.secrets.TGTGenerationSpecDTO;
import io.harness.ng.core.dto.secrets.TGTKeyTabFilePathSpecDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("KeyTabFilePath")
public class TGTKeyTabFilePathSpec extends TGTGenerationSpec {
  private String keyPath;

  @Override
  public TGTGenerationSpecDTO toDTO() {
    return TGTKeyTabFilePathSpecDTO.builder().keyPath(getKeyPath()).build();
  }
}
