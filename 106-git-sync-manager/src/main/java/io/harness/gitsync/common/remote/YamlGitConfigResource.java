package io.harness.gitsync.common.remote;

import static io.harness.gitsync.common.remote.YamlGitConfigMapper.applyUpdateToYamlGitConfigDTO;
import static io.harness.gitsync.common.remote.YamlGitConfigMapper.toSetupGitSyncDTO;
import static io.harness.gitsync.common.remote.YamlGitConfigMapper.toYamlGitConfigDTO;

import com.google.inject.Inject;

import io.harness.gitsync.common.dtos.GitSyncConfigDTO;
import io.harness.gitsync.common.dtos.YamlGitConfigDTO;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

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

@Api("/git-sync")
@Path("/git-sync")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class YamlGitConfigResource {
  private final YamlGitConfigService yamlGitConfigService;

  @POST
  @ApiOperation(value = "Create a Git Sync", nickname = "postGitSync")
  public GitSyncConfigDTO create(@NotNull @Valid GitSyncConfigDTO request) {
    YamlGitConfigDTO yamlGitConfig = yamlGitConfigService.save(toYamlGitConfigDTO(request));
    return toSetupGitSyncDTO(yamlGitConfig);
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update Git Sync by id", nickname = "putGitSync")
  public GitSyncConfigDTO update(@QueryParam("projectId") String projectId,
      @QueryParam("organizationId") String organizationId, @QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("identifier") String identifier, @NotNull @Valid GitSyncConfigDTO updateGitSyncConfigDTO) {
    YamlGitConfigDTO yamlGitConfigDTO =
        yamlGitConfigService.getByIdentifier(projectId, organizationId, accountId, identifier);
    if (yamlGitConfigDTO != null) {
      YamlGitConfigDTO yamlGitConfigDTOUpdated = yamlGitConfigService.update(
          applyUpdateToYamlGitConfigDTO(yamlGitConfigDTO, toYamlGitConfigDTO(updateGitSyncConfigDTO)));
      return toSetupGitSyncDTO(yamlGitConfigDTOUpdated);
    }
    return null;
  }

  @PUT
  @Path("{identifier}/folder/{folderIdentifier}/default")
  @ApiOperation(value = "Update Git Sync by id", nickname = "putGitSync")
  public List<GitSyncConfigDTO> updateDefault(@QueryParam("projectId") String projectId,
      @QueryParam("organizationId") String organizationId, @QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("identifier") @NotEmpty String identifier,
      @PathParam("folderIdentifier") @NotEmpty String folderIdentifier) {
    yamlGitConfigService.updateDefault(projectId, organizationId, accountId, identifier, folderIdentifier);
    return list(projectId, organizationId, accountId);
  }

  @GET
  @ApiOperation(value = "Get Git Sync list", nickname = "getGitSyncList")
  public List<GitSyncConfigDTO> list(@QueryParam("projectId") String projectId,
      @QueryParam("organizationId") String organizationId, @QueryParam("accountId") @NotEmpty String accountId) {
    List<YamlGitConfigDTO> yamlGitConfigDTOs = yamlGitConfigService.get(projectId, organizationId, accountId);
    return yamlGitConfigDTOs.stream().map(YamlGitConfigMapper::toSetupGitSyncDTO).collect(Collectors.toList());
  }
}
