package io.harness.pms.approval.resources;

import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.annotations.PipelineServiceAuth;
import io.harness.pms.approval.ApprovalResourceService;
import io.harness.steps.approval.step.beans.ApprovalInstanceResponseDTO;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivityRequestDTO;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalInstanceAuthorizationDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("approvals")
@Path("approvals")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@PipelineServiceAuth
@Slf4j
public class ApprovalResource {
  private final ApprovalResourceService approvalResourceService;

  @Inject
  public ApprovalResource(ApprovalResourceService approvalResourceService) {
    this.approvalResourceService = approvalResourceService;
  }

  @GET
  @Path("/{approvalInstanceId}")
  @ApiOperation(value = "Gets an approval instance by id", nickname = "getApprovalInstance")
  public ResponseDTO<ApprovalInstanceResponseDTO> getApprovalInstance(
      @NotEmpty @PathParam("approvalInstanceId") String approvalInstanceId) {
    return ResponseDTO.newResponse(approvalResourceService.get(approvalInstanceId));
  }

  @POST
  @Path("/{approvalInstanceId}/harness/activity")
  @ApiOperation(value = "Add a new Harness approval activity", nickname = "addHarnessApprovalActivity")
  public ResponseDTO<ApprovalInstanceResponseDTO> addHarnessApprovalActivity(
      @NotEmpty @PathParam("approvalInstanceId") String approvalInstanceId,
      @NotNull @Valid HarnessApprovalActivityRequestDTO request) {
    return ResponseDTO.newResponse(approvalResourceService.addHarnessApprovalActivity(approvalInstanceId, request));
  }

  @GET
  @Path("/{approvalInstanceId}/harness/authorization")
  @ApiOperation(value = "Gets a Harness approval instance authorization for the current user",
      nickname = "getHarnessApprovalInstanceAuthorization")
  public ResponseDTO<HarnessApprovalInstanceAuthorizationDTO>
  getHarnessApprovalInstanceAuthorization(@NotEmpty @PathParam("approvalInstanceId") String approvalInstanceId) {
    return ResponseDTO.newResponse(HarnessApprovalInstanceAuthorizationDTO.builder().authorized(true).build());
  }
}
