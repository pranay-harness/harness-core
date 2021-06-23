package io.harness.ng.serviceaccounts;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.SERVICEACCOUNT;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.accesscontrol.PlatformPermissions;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.serviceaccounts.dto.ServiceAccountRequestDTO;
import io.harness.ng.serviceaccounts.service.api.ServiceAccountService;
import io.harness.serviceaccount.ServiceAccountDTO;

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
  @Inject private final ServiceAccountService serviceAccountService;

  @POST
  @ApiOperation(value = "Create service account", nickname = "createServiceAccount")
  @NGAccessControlCheck(resourceType = SERVICEACCOUNT, permission = PlatformPermissions.EDIT_SERVICEACCOUNT_PERMISSION)
  public ResponseDTO<ServiceAccountDTO> createServiceAccount(
      @QueryParam("accountIdentifier") @AccountIdentifier String accountIdentifier,
      @Optional @QueryParam("orgIdentifier") @OrgIdentifier String orgIdentifier,
      @Optional @QueryParam("projectIdentifier") @ProjectIdentifier String projectIdentifier,
      @Valid ServiceAccountRequestDTO serviceAccountRequestDTO) {
    ServiceAccountDTO serviceAccountDTO = serviceAccountService.createServiceAccount(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceAccountRequestDTO);
    return ResponseDTO.newResponse(serviceAccountDTO);
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update service account", nickname = "updateServiceAccount")
  @NGAccessControlCheck(resourceType = SERVICEACCOUNT, permission = PlatformPermissions.EDIT_SERVICEACCOUNT_PERMISSION)
  public ResponseDTO<ServiceAccountDTO> updateServiceAccount(
      @QueryParam("accountIdentifier") @AccountIdentifier String accountIdentifier,
      @Optional @QueryParam("orgIdentifier") @OrgIdentifier String orgIdentifier,
      @Optional @QueryParam("projectIdentifier") @ProjectIdentifier String projectIdentifier,
      @PathParam("identifier") @ResourceIdentifier String identifier,
      @Valid ServiceAccountRequestDTO serviceAccountRequestDTO) {
    ServiceAccountDTO serviceAccountDTO = serviceAccountService.updateServiceAccount(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, serviceAccountRequestDTO);
    return ResponseDTO.newResponse(serviceAccountDTO);
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete service account", nickname = "deleteServiceAccount")
  @NGAccessControlCheck(
      resourceType = SERVICEACCOUNT, permission = PlatformPermissions.DELETE_SERVICEACCOUNT_PERMISSION)
  public ResponseDTO<Boolean>
  deleteServiceAccount(@QueryParam("accountIdentifier") @AccountIdentifier String accountIdentifier,
      @Optional @QueryParam("orgIdentifier") @OrgIdentifier String orgIdentifier,
      @Optional @QueryParam("projectIdentifier") @ProjectIdentifier String projectIdentifier,
      @PathParam("identifier") @ResourceIdentifier String identifier) {
    boolean deleted =
        serviceAccountService.deleteServiceAccount(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return ResponseDTO.newResponse(deleted);
  }

  @GET
  @ApiOperation(value = "List service account", nickname = "listServiceAccount")
  @NGAccessControlCheck(resourceType = SERVICEACCOUNT, permission = PlatformPermissions.VIEW_SERVICEACCOUNT_PERMISSION)
  public ResponseDTO<List<ServiceAccountDTO>> listServiceAccounts(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Optional @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Optional @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @Optional @QueryParam(NGResourceFilterConstants.IDENTIFIERS) List<String> identifiers) {
    List<ServiceAccountDTO> requestDTOS =
        serviceAccountService.listServiceAccounts(accountIdentifier, orgIdentifier, projectIdentifier, identifiers);
    return ResponseDTO.newResponse(requestDTOS);
  }
}
