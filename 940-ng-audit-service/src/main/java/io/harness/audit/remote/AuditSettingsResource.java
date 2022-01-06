/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.audit.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.mapper.AuditSettingsMapper.toDTO;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.api.AuditSettingsService;
import io.harness.audit.beans.AuditSettingsDTO;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.InternalApi;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@OwnedBy(PL)
@Api("auditSettings")
@Path("auditSettings")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class AuditSettingsResource {
  @Inject private final AuditSettingsService auditSettingsService;

  @PUT
  @ApiOperation(value = "Update Audit Settings for an account", nickname = "putAuditSettings")
  @InternalApi
  public ResponseDTO<AuditSettingsDTO> update(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY)
                                              String accountIdentifier, @NotNull AuditSettingsDTO auditSettingsDTO) {
    AuditSettingsDTO updatedAuditSettingsDTO =
        toDTO(auditSettingsService.update(accountIdentifier, auditSettingsDTO.getRetentionPeriodInMonths()));
    return ResponseDTO.newResponse(updatedAuditSettingsDTO);
  }
}
