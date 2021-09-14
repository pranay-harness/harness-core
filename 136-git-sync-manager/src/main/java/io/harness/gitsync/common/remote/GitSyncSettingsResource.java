/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.remote;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.ng.core.rbac.ProjectPermissions.EDIT_PROJECT_PERMISSION;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.accesscontrol.ResourceTypes;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.dtos.GitSyncSettingsDTO;
import io.harness.gitsync.common.service.GitSyncSettingsService;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

@Api("/git-sync-settings")
@Path("/git-sync-settings")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitSyncSettingsResource {
  private final GitSyncSettingsService gitSyncSettingsService;
  private final AccessControlClient accessControlClient;

  @POST
  @ApiOperation(value = "Create a Git Sync Setting", nickname = "postGitSyncSetting")
  public ResponseDTO<GitSyncSettingsDTO> create(@NotNull @Valid GitSyncSettingsDTO gitSyncSettings) {
    // todo(abhinav): when git sync comes at other level see for new permission
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(gitSyncSettings.getAccountIdentifier(), gitSyncSettings.getOrganizationIdentifier(),
            gitSyncSettings.getProjectIdentifier()),
        Resource.of(ResourceTypes.PROJECT, gitSyncSettings.getProjectIdentifier()), EDIT_PROJECT_PERMISSION);

    return ResponseDTO.newResponse(gitSyncSettingsService.save(gitSyncSettings));
  }

  @GET
  @ApiOperation(value = "Get git sync settings", nickname = "getGitSyncSettings")
  public ResponseDTO<GitSyncSettingsDTO> get(@QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String organizationIdentifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotEmpty String accountIdentifier) {
    final Optional<GitSyncSettingsDTO> gitSyncSettingsDTO =
        gitSyncSettingsService.get(accountIdentifier, organizationIdentifier, projectIdentifier);
    return gitSyncSettingsDTO.map(ResponseDTO::newResponse)
        .orElseThrow(
            ()
                -> new InvalidRequestException(String.format(
                    "No Git Sync Setting found for accountIdentifier %s, organizationIdentifier %s and projectIdentifier %s",
                    accountIdentifier, organizationIdentifier, projectIdentifier)));
  }

  @PUT
  @ApiOperation(value = "Update a Git Sync Setting", nickname = "updateGitSyncSetting")
  public ResponseDTO<GitSyncSettingsDTO> update(@NotNull @Valid GitSyncSettingsDTO gitSyncSettings) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(gitSyncSettings.getAccountIdentifier(), gitSyncSettings.getOrganizationIdentifier(),
            gitSyncSettings.getProjectIdentifier()),
        Resource.of(ResourceTypes.PROJECT, gitSyncSettings.getProjectIdentifier()), EDIT_PROJECT_PERMISSION);

    return ResponseDTO.newResponse(gitSyncSettingsService.update(gitSyncSettings));
  }
}
