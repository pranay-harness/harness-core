package io.harness.ng.serviceaccounts;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.serviceaccounts.dto.ServiceAccountRequestDTO;
import io.harness.ng.serviceaccounts.service.api.ServiceAccountService;

import com.google.inject.Inject;
import com.typesafe.config.Optional;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Api("serviceaccount")
@Path("serviceaccount")
@Produces({"application/json", "application/yaml", "text/plain"})
@Consumes({"application/json", "application/yaml", "text/plain"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
@OwnedBy(PL)
public class ServiceAccountResource {
  @Inject private ServiceAccountService serviceAccountService;

  @POST
  @ApiOperation(value = "Create service account", nickname = "createServiceAccount")
  public ResponseDTO<Void> createServiceAccount(@QueryParam("accountIdentifier") String accountIdentifier,
      @Optional @QueryParam("orgIdentifier") String orgIdentifier,
      @Optional @QueryParam("projectIdentifier") String projectIdentifier,
      @Valid ServiceAccountRequestDTO serviceAccountRequestDTO) {
    serviceAccountService.createServiceAccount(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceAccountRequestDTO);
    return ResponseDTO.newResponse(null);
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update service account", nickname = "updateServiceAccount")
  public ResponseDTO<Void> updateServiceAccount(@QueryParam("accountIdentifier") String accountIdentifier,
      @Optional @QueryParam("orgIdentifier") String orgIdentifier,
      @Optional @QueryParam("projectIdentifier") String projectIdentifier, @PathParam("identifier") String identifier,
      @Valid ServiceAccountRequestDTO serviceAccountRequestDTO) {
    serviceAccountService.updateServiceAccount(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, serviceAccountRequestDTO);
    return ResponseDTO.newResponse(null);
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete service account", nickname = "deleteServiceAccount")
  public ResponseDTO<Void> deleteServiceAccount(@QueryParam("accountIdentifier") String accountIdentifier,
      @Optional @QueryParam("orgIdentifier") String orgIdentifier,
      @Optional @QueryParam("projectIdentifier") String projectIdentifier, @PathParam("identifier") String identifier) {
    serviceAccountService.deleteServiceAccount(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return ResponseDTO.newResponse(null);
  }

  @GET
  @ApiOperation(value = "List service account", nickname = "listServiceAccount")
  public ResponseDTO<List<ServiceAccountRequestDTO>> listServiceAccounts(
      @QueryParam("accountIdentifier") String accountIdentifier,
      @Optional @QueryParam("orgIdentifier") String orgIdentifier,
      @Optional @QueryParam("projectIdentifier") String projectIdentifier) {
    List<ServiceAccountRequestDTO> requestDTOS =
        serviceAccountService.listServiceAccounts(accountIdentifier, orgIdentifier, projectIdentifier);
    return ResponseDTO.newResponse(requestDTOS);
  }
}
