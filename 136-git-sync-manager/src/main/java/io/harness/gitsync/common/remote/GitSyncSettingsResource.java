package io.harness.gitsync.common.remote;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.dtos.GitSyncSettingsDTO;
import io.harness.gitsync.common.service.GitSyncSettingsService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import lombok.AllArgsConstructor;
import retrofit2.http.Body;

@Api("/git-sync-settings")
@Path("/git-sync-settings")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitSyncSettingsResource {
  private final GitSyncSettingsService gitSyncSettingsService;

  @POST
  @ApiOperation(value = "Create a Git Sync Setting", nickname = "postGitSyncSetting")
  public GitSyncSettingsDTO create(@Body @NotNull GitSyncSettingsDTO request) {
    return gitSyncSettingsService.save(request);
  }
}
