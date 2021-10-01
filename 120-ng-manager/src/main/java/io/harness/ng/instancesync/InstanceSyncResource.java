package io.harness.ng.instancesync;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.beans.HarnessServiceInfoNG;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.instancesync.InstanceSyncPerpetualTaskResponse;
import io.harness.dtos.InstanceDTO;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.service.instancesync.InstanceSyncService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import retrofit2.http.Body;

@OwnedBy(HarnessTeam.DX)
@Api("instancesync")
@Path("instancesync")
@NextGenManagerAuth
@Produces({"application/json"})
@Consumes({"application/json"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class InstanceSyncResource {
  private final InstanceSyncService instanceSyncService;
  public static final String INSTANCE_INFO_POD_NAME = "instanceInfoPodName";
  public static final String INSTANCE_INFO_NAMESPACE = "instanceInfoNamespace";
  private final io.harness.service.instance.InstanceService instanceService;

  @POST
  @Path("/response")
  @ApiOperation(value = "Get instance sync perpetual task response", nickname = "getInstanceSyncPerpetualTaskResponse")
  public ResponseDTO<Boolean> processInstanceSyncPerpetualTaskResponse(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PERPETUAL_TASK_ID) String perpetualTaskId,
      @Body DelegateResponseData delegateResponseData) {
    InstanceSyncPerpetualTaskResponse instanceSyncPerpetualTaskResponse =
        (InstanceSyncPerpetualTaskResponse) delegateResponseData;
    log.info("Received instance sync perpetual task response for accountId : {} and perpetualTaskId : {} : {}",
        accountIdentifier, perpetualTaskId, instanceSyncPerpetualTaskResponse.toString());
    instanceSyncService.processInstanceSyncByPerpetualTask(
        accountIdentifier, perpetualTaskId, instanceSyncPerpetualTaskResponse);
    return ResponseDTO.newResponse(Boolean.TRUE);
  }
}
