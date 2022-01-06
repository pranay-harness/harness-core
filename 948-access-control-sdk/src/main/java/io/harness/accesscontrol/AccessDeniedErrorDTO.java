/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol;

import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ErrorDTO;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@OwnedBy(HarnessTeam.PL)
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel(value = "AccessControlCheckError")
@Schema(name = "AccessDeniedError")
public class AccessDeniedErrorDTO extends ErrorDTO {
  private List<PermissionCheckDTO> failedPermissionChecks;

  public AccessDeniedErrorDTO(Status status, ErrorCode code, String message, String detailedMessage,
      List<PermissionCheckDTO> failedPermissionChecks) {
    super(status, code, message, detailedMessage);
    this.failedPermissionChecks = failedPermissionChecks;
  }
}
