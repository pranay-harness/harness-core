/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.dto.secrets;

import io.harness.beans.DecryptableEntity;
import io.harness.ng.core.models.TGTGenerationSpec;
import io.harness.ng.core.models.TGTKeyTabFilePathSpec;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("KeyTabFilePath")
public class TGTKeyTabFilePathSpecDTO extends TGTGenerationSpecDTO implements DecryptableEntity {
  private String keyPath;

  @Override
  public TGTGenerationSpec toEntity() {
    return TGTKeyTabFilePathSpec.builder().keyPath(getKeyPath()).build();
  }
}
