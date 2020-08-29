package io.harness.gitsync.common.remote;

import com.google.inject.Inject;

import io.harness.beans.NGPageResponse;
import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;
import io.harness.gitsync.common.dtos.GitSyncProductDTO;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.gitsync.core.EntityType;
import io.harness.gitsync.core.Product;
import io.harness.ng.core.dto.ResponseDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

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
      @QueryParam("size") int size, @QueryParam("product") Product product) {
    return ResponseDTO.newResponse(gitEntityService.list(projectId, organizationId, accountId, product, size));
  }

  @GET
  @Path("entities/{entityType}")
  @ApiOperation(value = "Get Git Sync Entity By Type", nickname = "listGitSyncEntitiesByType")
  public ResponseDTO<NGPageResponse<GitSyncEntityListDTO>> listByType(@QueryParam("projectId") String projectId,
      @QueryParam("organizationId") String organizationId, @QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("entityType") EntityType entityType, @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") int size) {
    return ResponseDTO.newResponse(
        gitEntityService.getPageByType(projectId, organizationId, accountId, entityType, page, size));
  }
}
