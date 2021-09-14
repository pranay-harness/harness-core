/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.userprofile.resource;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.userprofile.commons.SourceCodeManagerDTO;
import io.harness.ng.userprofile.services.api.SourceCodeManagerService;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@OwnedBy(PL)
@Api("source-code-manager")
@Path("source-code-manager")
@Produces("application/json")
@NextGenManagerAuth
public class SourceCodeManagerResource {
  @Inject SourceCodeManagerService sourceCodeManagerService;

  @GET
  @ApiOperation(value = "get source code manager information", nickname = "getSourceCodeManagers")
  public ResponseDTO<List<SourceCodeManagerDTO>> get(
      @NotNull @QueryParam("accountIdentifier") String accountIdentifier) {
    return ResponseDTO.newResponse(sourceCodeManagerService.get(accountIdentifier));
  }

  @POST
  @ApiOperation(value = "save source code manager", nickname = "saveSourceCodeManagers")
  public ResponseDTO<SourceCodeManagerDTO> save(@NotNull @Body @Valid SourceCodeManagerDTO sourceCodeManagerDTO) {
    return ResponseDTO.newResponse(sourceCodeManagerService.save(sourceCodeManagerDTO));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "update source code manager", nickname = "updateSourceCodeManagers")
  public ResponseDTO<SourceCodeManagerDTO> update(@NotNull @PathParam("identifier") String sourceCodeManagerIdentifier,
      @NotNull @Body @Valid SourceCodeManagerDTO sourceCodeManagerDTO) {
    return ResponseDTO.newResponse(sourceCodeManagerService.update(sourceCodeManagerIdentifier, sourceCodeManagerDTO));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "delete source code manager", nickname = "deleteSourceCodeManagers")
  public ResponseDTO<Boolean> delete(@NotNull @PathParam("identifier") String sourceCodeManagerName,
      @NotNull @QueryParam("accountIdentifier") String accountIdentifier) {
    return ResponseDTO.newResponse(sourceCodeManagerService.delete(sourceCodeManagerName, accountIdentifier));
  }
}
