package software.wings.resources.governance;

import com.google.inject.Inject;

import io.harness.governance.pipeline.enforce.PipelineReportCard;
import io.harness.governance.pipeline.service.PipelineGovernanceReportEvaluator;
import io.harness.governance.pipeline.service.PipelineGovernanceService;
import io.harness.governance.pipeline.service.model.PipelineGovernanceConfig;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
  private PipelineGovernanceReportEvaluator pipelineGovernanceReportEvaluator;

  @Inject
  public PipelineGovernanceResource(PipelineGovernanceService pipelineGovernanceService,
      PipelineGovernanceReportEvaluator pipelineGovernanceReportEvaluator) {
    this.pipelineGovernanceService = pipelineGovernanceService;
    this.pipelineGovernanceReportEvaluator = pipelineGovernanceReportEvaluator;
  }

  @GET
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<List<PipelineGovernanceConfig>> list(@QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(pipelineGovernanceService.list(accountId));
  }

  @POST
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<PipelineGovernanceConfig> add(@QueryParam("accountId") @NotEmpty String accountId,
      @NotNull(message = "governanceConfig missing while trying to add pipeline governance config")
      PipelineGovernanceConfig governanceConfig) {
    return new RestResponse<>(pipelineGovernanceService.add(accountId, governanceConfig));
  }

  @DELETE
  @Path("{uuid}")
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<Boolean> update(
      @QueryParam("accountId") @NotEmpty String accountId, @PathParam("uuid") String uuid) {
    return new RestResponse<>(pipelineGovernanceService.delete(accountId, uuid));
  }

  @GET
  @Path("report")
  public RestResponse<List<PipelineReportCard>> report(@QueryParam("accountId") @NotEmpty final String accountId,
      @QueryParam("appId") final String appId, @QueryParam("pipelineId") final String pipelineId) {
    return new RestResponse<>(pipelineGovernanceReportEvaluator.getPipelineReportCard(accountId, appId, pipelineId));
  }
}
