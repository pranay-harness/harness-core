package io.harness.gitsync.common.remote;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.YamlConstants;
import io.harness.gitsync.common.service.GitBranchService;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;

@Api("/git-sync-branch")
@Path("/git-sync-branch")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitBranchResource {
  private final GitBranchService gitBranchService;

  @GET
  @Path("listBranchesByConnector")
  @ApiOperation(value = "Gets list of branches by Connector Identifier", nickname = "getListOfBranchesByConnector")
  public ResponseDTO<List<String>> listBranchesForRepo(
      @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String connectorIdentifier,
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.REPO_URL) String repoURL) {
    return ResponseDTO.newResponse(gitBranchService.listBranchesForRepoByConnector(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier, repoURL));
  }

  @GET
  @Path("listBranchesByGitConfig")
  @ApiOperation(value = "Gets list of branches by Git Config Identifier", nickname = "getListOfBranchesByGitConfig")
  public ResponseDTO<List<String>> listBranchesForRepo(
      @QueryParam(YamlConstants.YAML_GIT_CONFIG) String yamlGitConfigIdentifier,
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier) {
    return ResponseDTO.newResponse(gitBranchService.listBranchesForRepoByGitSyncConfig(
        accountIdentifier, orgIdentifier, projectIdentifier, yamlGitConfigIdentifier));
  }
}
