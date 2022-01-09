/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serviceaccount;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;

import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PL)
public class ServiceAccountDTO {
  @ApiModelProperty(required = true) @EntityIdentifier @NotBlank String identifier;
  @ApiModelProperty(required = true) @NotBlank String name;
  @ApiModelProperty(required = true) @Email @NotBlank String email;
  @Size(max = 1024) String description;
  @Size(max = 128) Map<String, String> tags;
  @ApiModelProperty(required = true) @NotBlank String accountIdentifier;
  @EntityIdentifier(allowBlank = true) String orgIdentifier;
  @EntityIdentifier(allowBlank = true) String projectIdentifier;
}
