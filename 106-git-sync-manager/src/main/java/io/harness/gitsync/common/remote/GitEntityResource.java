package io.harness.gitsync.common.remote;

import com.google.inject.Inject;

import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;
import io.harness.gitsync.common.service.GitEntityService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
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
  @ApiOperation(value = "List Git Sync Entity", nickname = "listGitSyncEntities")
  public List<GitSyncEntityListDTO> list(@QueryParam("projectId") String projectId,
      @QueryParam("organizationId") String organizationId, @QueryParam("accountId") @NotEmpty String accountId) {
    return gitEntityService.list(projectId, organizationId, accountId);
  }
}
