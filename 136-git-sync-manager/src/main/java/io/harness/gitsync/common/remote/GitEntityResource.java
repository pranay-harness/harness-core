package io.harness.gitsync.common.remote;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;
import io.harness.gitsync.common.dtos.GitSyncProductDTO;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

@Api("/git-sync-entities")
@Path("/git-sync-entities")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class GitEntityResource {
  private GitEntityService gitEntityService;

  @GET
  @ApiOperation(value = "List Git Sync Entity by product", nickname = "listGitSyncEntitiesByProduct")
  public ResponseDTO<GitSyncProductDTO> list(@QueryParam("projectId") String projectId,
      @QueryParam("organizationId") String organizationId, @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("size") int size, @QueryParam("moduleType") ModuleType moduleType) {
    return ResponseDTO.newResponse(gitEntityService.list(projectId, organizationId, accountId, moduleType, size));
  }

  @GET
  @Path("entities/{entityType}")
  @ApiOperation(value = "Get Git Sync Entity By Type", nickname = "listGitSyncEntitiesByType")
  public ResponseDTO<PageResponse<GitSyncEntityListDTO>> listByType(@QueryParam("projectId") String projectId,
      @QueryParam("organizationId") String organizationId, @QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("entityType") EntityType entityType, @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") int size, @QueryParam("moduleType") String moduleType) {
    // Added moduleType for now if in future we want to support product filter in entities as well.
    return ResponseDTO.newResponse(
        gitEntityService.getPageByType(projectId, organizationId, accountId, entityType, page, size));
  }
}
