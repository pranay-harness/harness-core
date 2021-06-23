package io.harness.ng.core.remote;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.IDENTIFIER_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;
import static io.harness.NGResourceFilterConstants.IDENTIFIERS;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.api.ApiKeyService;
import io.harness.ng.core.common.beans.ApiKeyType;
import io.harness.ng.core.dto.ApiKeyDTO;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;

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

@Api("apikey")
@Path("apikey")
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
public class ApiKeyResource {
  @Inject private ApiKeyService apiKeyService;

  @POST
  @ApiOperation(value = "Create api key", nickname = "createApiKey")
  public ResponseDTO<ApiKeyDTO> createApiKey(@Valid ApiKeyDTO apiKeyDTO) {
    ApiKeyDTO apiKey = apiKeyService.createApiKey(apiKeyDTO);
    return ResponseDTO.newResponse(apiKey);
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update api key", nickname = "updateApiKey")
  public ResponseDTO<ApiKeyDTO> updateApiKey(@Valid ApiKeyDTO apiKeyDTO, @PathParam("identifier") String identifier) {
    ApiKeyDTO apiKey = apiKeyService.updateApiKey(apiKeyDTO);
    return ResponseDTO.newResponse(apiKey);
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete api key", nickname = "deleteApiKey")
  public ResponseDTO<Boolean> deleteApiKey(@QueryParam(ACCOUNT_KEY) String accountIdentifier,
      @Optional @QueryParam(ORG_KEY) String orgIdentifier, @Optional @QueryParam(PROJECT_KEY) String projectIdentifier,
      @QueryParam("apiKeyType") ApiKeyType apiKeyType, @QueryParam("parentIdentifier") String parentIdentifier,
      @PathParam(IDENTIFIER_KEY) String identifier) {
    boolean deleted = apiKeyService.deleteApiKey(
        accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier, identifier);
    return ResponseDTO.newResponse(deleted);
  }

  @GET
  @ApiOperation(value = "List api keys", nickname = "listApiKeys")
  public ResponseDTO<List<ApiKeyDTO>> listApiKeys(@QueryParam(ACCOUNT_KEY) String accountIdentifier,
      @Optional @QueryParam(ORG_KEY) String orgIdentifier, @Optional @QueryParam(PROJECT_KEY) String projectIdentifier,
      @QueryParam("apiKeyType") ApiKeyType apiKeyType, @QueryParam("parentIdentifier") String parentIdentifier,
      @Optional @QueryParam(IDENTIFIERS) List<String> identifiers) {
    List<ApiKeyDTO> apiKeyDTOs = apiKeyService.listApiKeys(
        accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier, identifiers);
    return ResponseDTO.newResponse(apiKeyDTOs);
  }
}
