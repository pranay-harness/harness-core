package io.harness.ng.core.artifacts.resources.gcr;

import io.harness.NGCommonEntityConstants;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.resources.gcr.dtos.GcrBuildDetailsDTO;
import io.harness.cdng.artifact.resources.gcr.dtos.GcrRequestDTO;
import io.harness.cdng.artifact.resources.gcr.dtos.GcrResponseDTO;
import io.harness.cdng.artifact.resources.gcr.service.GcrResourceService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Api("artifacts")
@Path("/artifacts/gcr")
@Produces({"application/json"})
@Consumes({"application/json"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class GcrArtifactResource {
  private final GcrResourceService gcrResourceService;

  @GET
  @Path("getBuildDetails")
  @ApiOperation(value = "Gets gcr build details", nickname = "getBuildDetailsForGcr")
  public ResponseDTO<GcrResponseDTO> getBuildDetails(@NotNull @QueryParam("imagePath") String imagePath,
      @NotNull @QueryParam("registryHostname") String registryHostname,
      @NotNull @QueryParam("connectorRef") String gcrConnectorIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(gcrConnectorIdentifier, accountId, orgIdentifier, projectIdentifier);
    GcrResponseDTO buildDetails =
        gcrResourceService.getBuildDetails(connectorRef, imagePath, registryHostname, orgIdentifier, projectIdentifier);
    return ResponseDTO.newResponse(buildDetails);
  }

  @POST
  @Path("getLastSuccessfulBuild")
  @ApiOperation(value = "Gets gcr last successful build", nickname = "getLastSuccessfulBuildForGcr")
  public ResponseDTO<GcrBuildDetailsDTO> getLastSuccessfulBuild(@NotNull @QueryParam("imagePath") String imagePath,
      @NotNull @QueryParam("connectorRef") String gcrConnectorIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, GcrRequestDTO requestDTO) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(gcrConnectorIdentifier, accountId, orgIdentifier, projectIdentifier);
    GcrBuildDetailsDTO buildDetails =
        gcrResourceService.getSuccessfulBuild(connectorRef, imagePath, requestDTO, orgIdentifier, projectIdentifier);
    return ResponseDTO.newResponse(buildDetails);
  }

  @GET
  @Path("validateArtifactServer")
  @ApiOperation(value = "Validate gcr artifact server", nickname = "validateArtifactServerForGcr")
  public ResponseDTO<Boolean> validateArtifactServer(@NotNull @QueryParam("imagePath") String imagePath,
      @NotNull @QueryParam("connectorRef") String gcrConnectorIdentifier,
      @NotNull @QueryParam("registryHostname") String registryHostname,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(gcrConnectorIdentifier, accountId, orgIdentifier, projectIdentifier);
    boolean isValidArtifactServer = gcrResourceService.validateArtifactServer(
        connectorRef, imagePath, orgIdentifier, projectIdentifier, registryHostname);
    return ResponseDTO.newResponse(isValidArtifactServer);
  }

  @GET
  @Path("validateArtifactSource")
  @ApiOperation(value = "Validate docker image", nickname = "validateArtifactImageForGcr")
  public ResponseDTO<Boolean> validateArtifactImage(@NotNull @QueryParam("imagePath") String imagePath,
      @NotNull @QueryParam("registryHostname") String registryHostname,
      @NotNull @QueryParam("connectorRef") String gcrConnectorIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(gcrConnectorIdentifier, accountId, orgIdentifier, projectIdentifier);
    boolean isValidArtifactImage = gcrResourceService.validateArtifactSource(
        imagePath, connectorRef, registryHostname, orgIdentifier, projectIdentifier);
    return ResponseDTO.newResponse(isValidArtifactImage);
  }
}
