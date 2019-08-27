package software.wings.resources.governance;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.governance.pipeline.model.PipelineGovernanceConfig;
import io.harness.governance.pipeline.service.PipelineGovernanceService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api("pipeline-compliance")
@Path("/compliance/pipeline")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Scope(ResourceType.SETTING)
public class PipelineGovernanceResource {
  private PipelineGovernanceService pipelineGovernanceService;

  @Inject
  public PipelineGovernanceResource(PipelineGovernanceService pipelineGovernanceService) {
    this.pipelineGovernanceService = pipelineGovernanceService;
  }

  @GET
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<List<PipelineGovernanceConfig>> list(@QueryParam("accountId") String accountId) {
    if (StringUtils.isEmpty(accountId)) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "accountId missing while trying to get pipeline governance config");
    }

    return new RestResponse<>(pipelineGovernanceService.list(accountId));
  }

  @POST
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<PipelineGovernanceConfig> add(
      @QueryParam("accountId") String accountId, PipelineGovernanceConfig governanceConfig) {
    if (StringUtils.isEmpty(accountId)) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "accountId missing while trying to update pipeline governance config");
    }

    if (null == governanceConfig) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "governanceConfig missing while trying to update pipeline governance config");
    }

    return new RestResponse<>(pipelineGovernanceService.add(governanceConfig));
  }

  @PUT
  @Path("{uuid}")
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<PipelineGovernanceConfig> update(@QueryParam("accountId") String accountId,
      @PathParam("uuid") String uuid, PipelineGovernanceConfig governanceConfig) {
    if (StringUtils.isEmpty(accountId)) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "accountId missing while trying to update pipeline governance config");
    }

    if (null == governanceConfig) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "governanceConfig missing while trying to update pipeline governance config");
    }

    return new RestResponse<>(pipelineGovernanceService.update(uuid, governanceConfig));
  }
}
