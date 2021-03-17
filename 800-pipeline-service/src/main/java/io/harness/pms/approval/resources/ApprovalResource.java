package io.harness.pms.approval.resources;

import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.annotations.PipelineServiceAuth;
import io.harness.pms.approval.beans.ApprovalInstanceAuthorizationDTO;
import io.harness.pms.approval.beans.ApprovalInstanceResponseDTO;
import io.harness.pms.approval.beans.ApprovalType;
import io.harness.pms.approval.beans.HarnessApprovalActivityRequestDTO;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("approvals")
@Path("approvals")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@PipelineServiceAuth
@Slf4j
public class ApprovalResource {
  @GET
  @Path("/{approvalInstanceId}")
  @ApiOperation(value = "Gets an approval instance by id", nickname = "getApprovalInstance")
  public ResponseDTO<ApprovalInstanceResponseDTO> getApprovalInstance(
      @NotEmpty @PathParam("approvalInstanceId") String approvalInstanceId) {
    return ResponseDTO.newResponse(null);
  }

  @POST
  @Path("/{approvalInstanceId}/harness/activity")
  @ApiOperation(value = "Add a new Harness approval activity", nickname = "addHarnessApprovalActivity")
  public ResponseDTO<ApprovalInstanceResponseDTO> addHarnessApprovalActivity(
      @NotNull @Valid HarnessApprovalActivityRequestDTO request) {
    return ResponseDTO.newResponse(null);
  }

  @GET
  @Path("/{approvalInstanceId}/harness/authorization")
  @ApiOperation(value = "Gets a Harness approval instance authorization for the current user",
      nickname = "getHarnessApprovalInstanceAuthorization")
  public ResponseDTO<ApprovalInstanceAuthorizationDTO>
  getHarnessApprovalInstanceAuthorization(@NotEmpty @PathParam("approvalInstanceId") String approvalInstanceId) {
    return ResponseDTO.newResponse(ApprovalInstanceAuthorizationDTO.builder().authorized(true).build());
  }

  @GET
  @Path("/stage-yaml-snippet")
  @ApiOperation(value = "Gets the initial yaml snippet for approval stage", nickname = "getInitialStageYamlSnippet")
  public ResponseDTO<String> getInitialStageYamlSnippet(@NotNull @QueryParam("approvalType") ApprovalType approvalType)
      throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    return ResponseDTO.newResponse(
        Resources.toString(Objects.requireNonNull(classLoader.getResource(
                               String.format("approval_stage_yamls/%s.yaml", approvalType.getDisplayName()))),
            StandardCharsets.UTF_8));
  }
}
