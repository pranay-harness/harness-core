package io.harness.gitsync.common.remote;

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

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@OwnedBy(DX)
public class YamlGitConfigResource {
  private final YamlGitConfigService yamlGitConfigService;
  private final HarnessToGitHelperService harnessToGitHelperService;
  private final AccessControlClient accessControlClient;
  private final GitEnabledHelper gitEnabledHelper;
  @POST
  @ApiOperation(value = "Create a Git Sync", nickname = "postGitSync")
  public GitSyncConfigDTO create(
      @QueryParam("accountIdentifier") @NotEmpty String accountId, @NotNull @Valid GitSyncConfigDTO request) {
    // todo(abhinav): when git sync comes at other level see for new permission
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountId, request.getOrgIdentifier(), request.getProjectIdentifier()),
        Resource.of(ResourceTypes.PROJECT, request.getProjectIdentifier()), EDIT_PROJECT_PERMISSION);

    YamlGitConfigDTO yamlGitConfig = yamlGitConfigService.save(toYamlGitConfigDTO(request, accountId));
    return toSetupGitSyncDTO(yamlGitConfig);
  }

  @PUT
  @ApiOperation(value = "Update Git Sync by id", nickname = "putGitSync")
  public GitSyncConfigDTO update(@QueryParam("accountIdentifier") @NotEmpty String accountId,
      @NotNull @Valid GitSyncConfigDTO updateGitSyncConfigDTO) {
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
  public GitSyncConfigDTO updateDefault(@QueryParam("projectId") String projectId,
      @QueryParam("organizationId") String organizationId, @QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("identifier") @NotEmpty String identifier,
      @PathParam("folderIdentifier") @NotEmpty String folderIdentifier) {
    YamlGitConfigDTO yamlGitConfigDTO =
        yamlGitConfigService.updateDefault(projectId, organizationId, accountId, identifier, folderIdentifier);
    return toSetupGitSyncDTO(yamlGitConfigDTO);
  }

  @GET
  @ApiOperation(value = "List Git Sync", nickname = "listGitSync")
  public List<GitSyncConfigDTO> list(@QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String organizationId,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotEmpty String accountId) {
    List<YamlGitConfigDTO> yamlGitConfigDTOs = yamlGitConfigService.list(projectId, organizationId, accountId);
    return yamlGitConfigDTOs.stream().map(YamlGitConfigMapper::toSetupGitSyncDTO).collect(Collectors.toList());
  }

  @GET
  @Path("/git-sync-enabled")
  @ApiOperation(value = "Is Git Sync EnabledForProject", nickname = "isGitSyncEnabled")
  public GitEnabledDTO isGitSyncEnabled(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotEmpty String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String organizationIdentifier) {
    return gitEnabledHelper.getGitEnabledDTO(projectIdentifier, organizationIdentifier, accountIdentifier);
  }
}
