/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.dto;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.gitsync.beans.YamlDTO;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@OwnedBy(DEL)
public class DelegateNgTokenDTO implements YamlDTO {
  @ApiModelProperty(required = true) @EntityIdentifier private String identifier;
  @ApiModelProperty(required = true) @NotEmpty private String name;

  @ApiModelProperty(required = true) private String accountIdentifier;
  @EntityIdentifier(allowBlank = true) private String projectIdentifier;
  @EntityIdentifier(allowBlank = true) private String orgIdentifier;
}
