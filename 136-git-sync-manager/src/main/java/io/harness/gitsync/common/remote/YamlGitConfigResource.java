/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.remote;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE;
import static io.harness.NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE;
import static io.harness.NGCommonEntityConstants.BAD_REQUEST_CODE;
import static io.harness.NGCommonEntityConstants.BAD_REQUEST_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.INTERNAL_SERVER_ERROR_CODE;
import static io.harness.NGCommonEntityConstants.INTERNAL_SERVER_ERROR_MESSAGE;
import static io.harness.NGCommonEntityConstants.ORG_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PROJECT_PARAM_MESSAGE;
import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.common.remote.YamlGitConfigMapper.toSetupGitSyncDTO;
import static io.harness.gitsync.common.remote.YamlGitConfigMapper.toYamlGitConfigDTO;
import static io.harness.ng.core.rbac.ProjectPermissions.EDIT_PROJECT_PERMISSION;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.accesscontrol.ResourceTypes;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.common.dtos.GitEnabledDTO;
import io.harness.gitsync.common.dtos.GitSyncConfigDTO;
import io.harness.gitsync.common.helper.GitEnabledHelper;
import io.harness.gitsync.common.service.HarnessToGitHelperService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.sdk.GitSyncApiConstants;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

@Api("/git-sync")
@Path("/git-sync")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Tag(name = "Git Sync", description = "Contains APIs for CRUD on Git Sync")
@io.swagger.v3.oas.annotations.responses.
ApiResponse(responseCode = BAD_REQUEST_CODE, description = BAD_REQUEST_PARAM_MESSAGE,
    content =
    {
      @Content(mediaType = APPLICATION_JSON_MEDIA_TYPE, schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = APPLICATION_YAML_MEDIA_TYPE, schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.
ApiResponse(responseCode = INTERNAL_SERVER_ERROR_CODE, description = INTERNAL_SERVER_ERROR_MESSAGE,
    content =
    {
      @Content(mediaType = APPLICATION_JSON_MEDIA_TYPE, schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = APPLICATION_YAML_MEDIA_TYPE, schema = @Schema(implementation = ErrorDTO.class))
    })
@OwnedBy(DX)
public class YamlGitConfigResource {
  private final YamlGitConfigService yamlGitConfigService;
  private final HarnessToGitHelperService harnessToGitHelperService;
  private final AccessControlClient accessControlClient;
  private final GitEnabledHelper gitEnabledHelper;
  @POST
  @ApiOperation(value = "Create a Git Sync", nickname = "postGitSync")
  @Operation(operationId = "createGitSyncConfig", summary = "Creates Git Sync Config in given scope",
      responses =
      { @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "Successfully created Git Sync Config") })
  public GitSyncConfigDTO
  create(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam("accountIdentifier") @NotEmpty String accountId,
      @RequestBody(
          required = true, description = "Details of Git Sync Config") @NotNull @Valid GitSyncConfigDTO request) {
    // todo(abhinav): when git sync comes at other level see for new permission
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountId, request.getOrgIdentifier(), request.getProjectIdentifier()),
        Resource.of(ResourceTypes.PROJECT, request.getProjectIdentifier()), EDIT_PROJECT_PERMISSION);

    YamlGitConfigDTO yamlGitConfig = yamlGitConfigService.save(toYamlGitConfigDTO(request, accountId));
    return toSetupGitSyncDTO(yamlGitConfig);
  }

  @PUT
  @ApiOperation(value = "Update Git Sync by id", nickname = "putGitSync")
  @Operation(operationId = "updateGitSyncConfig", summary = "Update existing Git Sync Config by Identifier",
      responses = { @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "Updated Git Sync Config") })
  public GitSyncConfigDTO
  update(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam("accountIdentifier") @NotEmpty String accountId,
      @RequestBody(required = true,
          description = "Details of Git Sync Config") @NotNull @Valid GitSyncConfigDTO updateGitSyncConfigDTO) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, updateGitSyncConfigDTO.getOrgIdentifier(),
                                                  updateGitSyncConfigDTO.getProjectIdentifier()),
        Resource.of(ResourceTypes.PROJECT, updateGitSyncConfigDTO.getProjectIdentifier()), EDIT_PROJECT_PERMISSION);

    YamlGitConfigDTO yamlGitConfigDTOUpdated =
        yamlGitConfigService.update(toYamlGitConfigDTO(updateGitSyncConfigDTO, accountId));
    return toSetupGitSyncDTO(yamlGitConfigDTOUpdated);
  }

  @PUT
  @Path("{identifier}/folder/{folderIdentifier}/default")
  @ApiOperation(value = "Update Git Sync default by id", nickname = "putGitSyncDefault")
  @Operation(operationId = "updateDefaultFolder",
      summary = "Update existing Git Sync Config default root folder by Identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Updated Git Sync Config default root folder")
      })
  public GitSyncConfigDTO
  updateDefault(@Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam("projectId") String projectId,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam("organizationId") String organizationId,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam("accountId") @NotEmpty String accountId,
      @Parameter(description = GitSyncApiConstants.REPOID_PARAM_MESSAGE) @PathParam(
          "identifier") @NotEmpty String identifier,
      @Parameter(description = "Folder Id") @PathParam("folderIdentifier") @NotEmpty String folderIdentifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, organizationId, projectId),
        Resource.of(ResourceTypes.PROJECT, projectId), EDIT_PROJECT_PERMISSION);

    YamlGitConfigDTO yamlGitConfigDTO =
        yamlGitConfigService.updateDefault(projectId, organizationId, accountId, identifier, folderIdentifier);
    return toSetupGitSyncDTO(yamlGitConfigDTO);
  }

  @GET
  @ApiOperation(value = "List Git Sync", nickname = "listGitSync")
  @Operation(operationId = "getGitSyncConfigList", summary = "Lists Git Sync Config for the given scope",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "List of Git Sync Config for the given scope")
      })
  public List<GitSyncConfigDTO>
  list(
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String organizationId,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @NotEmpty String accountId) {
    List<YamlGitConfigDTO> yamlGitConfigDTOs = yamlGitConfigService.list(projectId, organizationId, accountId);
    return yamlGitConfigDTOs.stream().map(YamlGitConfigMapper::toSetupGitSyncDTO).collect(Collectors.toList());
  }

  @GET
  @Path("/git-sync-enabled")
  @ApiOperation(value = "Is Git Sync EnabledForProject", nickname = "isGitSyncEnabled")
  @Operation(operationId = "isGitSyncEnabled", summary = "Check whether Git Sync is enabled for given scope or not",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Result of whether Git Sync is enabled for the scope")
      })
  public GitEnabledDTO
  isGitSyncEnabled(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
                       NGCommonEntityConstants.ACCOUNT_KEY) @NotEmpty String accountIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) String organizationIdentifier) {
    return gitEnabledHelper.getGitEnabledDTO(projectIdentifier, organizationIdentifier, accountIdentifier);
  }
}
