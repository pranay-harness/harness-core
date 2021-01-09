package io.harness.connector.apis.resource;

import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.exception.WingsException.USER;
import static io.harness.utils.PageUtils.getNGPageResponse;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.connector.ConnectorCategory;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.apis.dto.ConnectorCatalogueResponseDTO;
import io.harness.connector.apis.dto.ConnectorFilterPropertiesDTO;
import io.harness.connector.apis.dto.stats.ConnectorStatistics;
import io.harness.connector.services.ConnectorService;
import io.harness.data.validator.EntityIdentifier;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;

@Api("/connectors")
@Path("/connectors")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class ConnectorResource {
  private static final String INCLUDE_ALL_CONNECTORS_ACCESSIBLE = "includeAllConnectorsAvailableAtScope";
  private final ConnectorService connectorService;
  private static final String CATEGORY_KEY = "category";

  @Inject
  public ConnectorResource(@Named("connectorDecoratorService") ConnectorService connectorService,
      ProjectService projectService, OrganizationService organizationService) {
    this.connectorService = connectorService;
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get Connector", nickname = "getConnector")
  public ResponseDTO<ConnectorResponseDTO> get(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @OrgIdentifier @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @ProjectIdentifier @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @EntityIdentifier @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String connectorIdentifier) {
    Optional<ConnectorResponseDTO> connectorResponseDTO =
        connectorService.get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    if (!connectorResponseDTO.isPresent()) {
      throw new NotFoundException(String.format("Connector with identifier [%s] in project [%s], org [%s] not found",
          connectorIdentifier, projectIdentifier, orgIdentifier));
    }
    return ResponseDTO.newResponse(connectorResponseDTO.get());
  }

  @GET
  @Path("validateUniqueIdentifier")
  @ApiOperation(value = "Validate Identifier is unique", nickname = "validateTheIdentifierIsUnique")
  public ResponseDTO<Boolean> validateTheIdentifierIsUnique(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) @EntityIdentifier String connectorIdentifier) {
    return ResponseDTO.newResponse(connectorService.validateTheIdentifierIsUnique(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
  }

  @GET
  @Deprecated
  @ApiOperation(value = "Gets Connector list", nickname = "getConnectorList")
  public ResponseDTO<PageResponse<ConnectorResponseDTO>> list(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam(NGResourceFilterConstants.TYPE_KEY) ConnectorType type,
      @QueryParam(CATEGORY_KEY) ConnectorCategory category) {
    return ResponseDTO.newResponse(getNGPageResponse(connectorService.list(
        page, size, accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, type, category)));
  }

  @POST
  @Path("/listV2")
  @ApiOperation(value = "Gets Connector list", nickname = "getConnectorListV2")
  public ResponseDTO<PageResponse<ConnectorResponseDTO>> list(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.FILTER_KEY) String filterIdentifier,
      @QueryParam(INCLUDE_ALL_CONNECTORS_ACCESSIBLE) Boolean includeAllConnectorsAccessibleAtScope,
      ConnectorFilterPropertiesDTO connectorListFilter) {
    return ResponseDTO.newResponse(
        getNGPageResponse(connectorService.list(page, size, accountIdentifier, connectorListFilter, orgIdentifier,
            projectIdentifier, filterIdentifier, searchTerm, includeAllConnectorsAccessibleAtScope)));
  }

  @POST
  @ApiOperation(value = "Creates a Connector", nickname = "createConnector")
  public ResponseDTO<ConnectorResponseDTO> create(@Valid @NotNull ConnectorDTO connector,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    if (HARNESS_SECRET_MANAGER_IDENTIFIER.equals(connector.getConnectorInfo().getIdentifier())) {
      throw new InvalidRequestException(
          String.format("%s cannot be used as connector identifier", HARNESS_SECRET_MANAGER_IDENTIFIER), USER);
    }
    if (connector.getConnectorInfo().getConnectorType() == ConnectorType.LOCAL) {
      throw new InvalidRequestException("Local Secret Manager creation not supported", USER);
    }
    return ResponseDTO.newResponse(connectorService.create(connector, accountIdentifier));
  }

  @PUT
  @ApiOperation(value = "Updates a Connector", nickname = "updateConnector")
  public ResponseDTO<ConnectorResponseDTO> update(@NotNull @Valid ConnectorDTO connector,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    if (HARNESS_SECRET_MANAGER_IDENTIFIER.equals(connector.getConnectorInfo().getIdentifier())) {
      throw new InvalidRequestException(
          String.format("Update operation not supported for Harness Secret Manager (identifier: [%s])",
              connector.getConnectorInfo().getIdentifier()));
    }
    return ResponseDTO.newResponse(connectorService.update(connector, accountIdentifier));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a connector by identifier", nickname = "deleteConnector")
  public ResponseDTO<Boolean> delete(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @NotNull @EntityIdentifier String connectorIdentifier) {
    if (HARNESS_SECRET_MANAGER_IDENTIFIER.equals(connectorIdentifier)) {
      throw new InvalidRequestException(String.format(
          "Delete operation not supported for Harness Secret Manager (identifier: [%s])", connectorIdentifier));
    }
    return ResponseDTO.newResponse(
        connectorService.delete(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
  }

  @POST
  @Path("validate")
  @Deprecated
  @ApiOperation(value = "Get the connectivity status of the Connector", nickname = "getConnectorStatus")
  public ResponseDTO<ConnectorValidationResult> validate(
      ConnectorDTO connector, @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    return ResponseDTO.newResponse(connectorService.validate(connector, accountIdentifier));
  }

  @POST
  @Path("testConnection/{identifier}")
  @ApiOperation(value = "Test the connection", nickname = "getTestConnectionResult")
  public ResponseDTO<ConnectorValidationResult> testConnection(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String connectorIdentifier) {
    return ResponseDTO.newResponse(
        connectorService.testConnection(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
  }

  @POST
  @Path("testGitRepoConnection/{identifier}")
  @ApiOperation(value = "Test the connection", nickname = "getTestGitRepoConnectionResult")
  public ResponseDTO<ConnectorValidationResult> testGitRepoConnection(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.REPO_URL) String repoURL,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @EntityIdentifier String connectorIdentifier) {
    return ResponseDTO.newResponse(connectorService.testGitRepoConnection(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier, repoURL));
  }

  @GET
  @Path("catalogue")
  @ApiOperation(value = "Get Connector Catalogue", nickname = "getConnectorCatalogue")
  public ResponseDTO<ConnectorCatalogueResponseDTO> getConnectorCatalogue(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    return ResponseDTO.newResponse(connectorService.getConnectorCatalogue());
  }

  @GET
  @Path("/stats")
  @ApiOperation(value = "Get Connectors statistics", nickname = "getConnectorStatistics")
  public ResponseDTO<ConnectorStatistics> getConnectorStats(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    return ResponseDTO.newResponse(
        connectorService.getConnectorStatistics(accountIdentifier, orgIdentifier, projectIdentifier));
  }
}
