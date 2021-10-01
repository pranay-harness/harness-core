package io.harness.connector.apis.resource;

import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.connector.accesscontrol.ConnectorsAccessControlPermissions.DELETE_CONNECTOR_PERMISSION;
import static io.harness.connector.accesscontrol.ConnectorsAccessControlPermissions.EDIT_CONNECTOR_PERMISSION;
import static io.harness.connector.accesscontrol.ConnectorsAccessControlPermissions.VIEW_CONNECTOR_PERMISSION;
import static io.harness.exception.WingsException.USER;
import static io.harness.ng.core.utils.URLDecoderUtility.getDecodedString;
import static io.harness.utils.PageUtils.getNGPageResponse;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorCatalogueResponseDTO;
import io.harness.connector.ConnectorCategory;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.accesscontrol.ResourceTypes;
import io.harness.connector.helper.ConnectorRbacHelper;
import io.harness.connector.services.ConnectorHeartbeatService;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.stats.ConnectorStatistics;
import io.harness.connector.utils.ConnectorAllowedFieldValues;
import io.harness.connector.utils.FieldValues;
import io.harness.data.validator.EntityIdentifier;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.exception.ConnectorNotFoundException;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.interceptor.GitEntityCreateInfoDTO;
import io.harness.gitsync.interceptor.GitEntityDeleteInfoDTO;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.gitsync.interceptor.GitEntityUpdateInfoDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.remote.CEAwsSetupConfig;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
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
import org.hibernate.validator.constraints.NotBlank;
import retrofit2.http.Body;

@Api("/connectors")
@Path("/connectors")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
@OwnedBy(HarnessTeam.DX)
public class ConnectorResource {
  private static final String INCLUDE_ALL_CONNECTORS_ACCESSIBLE = "includeAllConnectorsAvailableAtScope";
  private final ConnectorService connectorService;
  private final ConnectorHeartbeatService connectorHeartbeatService;
  private final CEAwsSetupConfig ceAwsSetupConfig;
  private static final String CATEGORY_KEY = "category";
  private static final String SOURCE_CATEGORY_KEY = "source_category";
  private final AccessControlClient accessControlClient;
  private ConnectorRbacHelper connectorRbacHelper;

  @Inject
  public ConnectorResource(@Named("connectorDecoratorService") ConnectorService connectorService,
      ConnectorHeartbeatService connectorHeartbeatService, CEAwsSetupConfig ceAwsSetupConfig,
      AccessControlClient accessControlClient, ConnectorRbacHelper connectorRbacHelper) {
    this.connectorService = connectorService;
    this.connectorHeartbeatService = connectorHeartbeatService;
    this.ceAwsSetupConfig = ceAwsSetupConfig;
    this.accessControlClient = accessControlClient;
    this.connectorRbacHelper = connectorRbacHelper;
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get Connector", nickname = "getConnector")
  @NGAccessControlCheck(resourceType = ResourceTypes.CONNECTOR, permission = VIEW_CONNECTOR_PERMISSION)
  public ResponseDTO<ConnectorResponseDTO> get(
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @OrgIdentifier @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @io.harness.accesscontrol.OrgIdentifier String orgIdentifier,
      @ProjectIdentifier @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @io.harness.accesscontrol.ProjectIdentifier String projectIdentifier,
      @EntityIdentifier @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY)
      @ResourceIdentifier String connectorIdentifier, @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
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
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) @EntityIdentifier String connectorIdentifier) {
    if (HARNESS_SECRET_MANAGER_IDENTIFIER.equals(connectorIdentifier)) {
      return ResponseDTO.newResponse(false);
    }
    return ResponseDTO.newResponse(connectorService.validateTheIdentifierIsUnique(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
  }

  @GET
  @ApiOperation(value = "Gets Connector list", nickname = "getConnectorList")
  @Deprecated
  public ResponseDTO<PageResponse<ConnectorResponseDTO>> list(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam(NGResourceFilterConstants.TYPE_KEY) ConnectorType type,
      @QueryParam(CATEGORY_KEY) ConnectorCategory category,
      @QueryParam(SOURCE_CATEGORY_KEY) ConnectorCategory sourceCategory,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(ResourceTypes.CONNECTOR, null), VIEW_CONNECTOR_PERMISSION);
    return ResponseDTO.newResponse(getNGPageResponse(connectorService.list(
        page, size, accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, type, category, sourceCategory)));
  }

  @POST
  @Path("/listV2")
  @ApiOperation(value = "Gets Connector list", nickname = "getConnectorListV2")
  public ResponseDTO<PageResponse<ConnectorResponseDTO>> list(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.FILTER_KEY) String filterIdentifier,
      @QueryParam(INCLUDE_ALL_CONNECTORS_ACCESSIBLE) Boolean includeAllConnectorsAccessibleAtScope,
      @Body ConnectorFilterPropertiesDTO connectorListFilter, @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
      @QueryParam("getDistinctFromBranches") Boolean getDistinctFromBranches) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(ResourceTypes.CONNECTOR, null), VIEW_CONNECTOR_PERMISSION);
    return ResponseDTO.newResponse(getNGPageResponse(
        connectorService.list(page, size, accountIdentifier, connectorListFilter, orgIdentifier, projectIdentifier,
            filterIdentifier, searchTerm, includeAllConnectorsAccessibleAtScope, getDistinctFromBranches)));
  }

  @POST
  @ApiOperation(value = "Creates a Connector", nickname = "createConnector")
  public ResponseDTO<ConnectorResponseDTO> create(@Valid @NotNull ConnectorDTO connector,
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @BeanParam GitEntityCreateInfoDTO gitEntityCreateInfo) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, connector.getConnectorInfo().getOrgIdentifier(),
            connector.getConnectorInfo().getProjectIdentifier()),
        Resource.of(ResourceTypes.CONNECTOR, null), EDIT_CONNECTOR_PERMISSION);

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
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @BeanParam GitEntityUpdateInfoDTO gitEntityInfo) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, connector.getConnectorInfo().getOrgIdentifier(),
            connector.getConnectorInfo().getProjectIdentifier()),
        Resource.of(ResourceTypes.CONNECTOR, connector.getConnectorInfo().getIdentifier()), EDIT_CONNECTOR_PERMISSION);
    if (HARNESS_SECRET_MANAGER_IDENTIFIER.equals(connector.getConnectorInfo().getIdentifier())) {
      throw new InvalidRequestException("Update operation not supported for Harness Secret Manager");
    }
    return ResponseDTO.newResponse(connectorService.update(connector, accountIdentifier));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a connector by identifier", nickname = "deleteConnector")
  @NGAccessControlCheck(resourceType = ResourceTypes.CONNECTOR, permission = DELETE_CONNECTOR_PERMISSION)
  public ResponseDTO<Boolean> delete(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier,
      @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier @io.harness.accesscontrol.OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier @io.harness.accesscontrol.
      ProjectIdentifier String projectIdentifier,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @NotBlank @ResourceIdentifier String connectorIdentifier,
      @BeanParam GitEntityDeleteInfoDTO entityDeleteInfo) {
    if (HARNESS_SECRET_MANAGER_IDENTIFIER.equals(connectorIdentifier)) {
      throw new InvalidRequestException("Delete operation not supported for Harness Secret Manager");
    }
    return ResponseDTO.newResponse(
        connectorService.delete(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
  }

  @POST
  @Path("testConnection/{identifier}")
  @ApiOperation(value = "Test the connection", nickname = "getTestConnectionResult")
  public ResponseDTO<ConnectorValidationResult> testConnection(
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String connectorIdentifier,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    connectorService.get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier)
        .map(connector
            -> connectorRbacHelper.checkSecretRuntimeAccessWithConnectorDTO(
                connector.getConnector(), accountIdentifier))
        .orElseThrow(()
                         -> new ConnectorNotFoundException(
                             String.format("No connector found with identifier %s", connectorIdentifier), USER));

    return ResponseDTO.newResponse(
        connectorService.testConnection(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
  }

  @POST
  @Path("testGitRepoConnection/{identifier}")
  @ApiOperation(value = "Test the connection", nickname = "getTestGitRepoConnectionResult")
  public ResponseDTO<ConnectorValidationResult> testGitRepoConnection(
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.REPO_URL) String repoURL,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @EntityIdentifier String connectorIdentifier) {
    return ResponseDTO.newResponse(connectorService.testGitRepoConnection(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier, getDecodedString(repoURL)));
  }

  @GET
  @Path("catalogue")
  @ApiOperation(value = "Get Connector Catalogue", nickname = "getConnectorCatalogue")
  public ResponseDTO<ConnectorCatalogueResponseDTO> getConnectorCatalogue(
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    return ResponseDTO.newResponse(connectorService.getConnectorCatalogue());
  }

  @GET
  @Path("/stats")
  @ApiOperation(value = "Get Connectors statistics", nickname = "getConnectorStatistics")
  public ResponseDTO<ConnectorStatistics> getConnectorStats(
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(ResourceTypes.CONNECTOR, null), VIEW_CONNECTOR_PERMISSION);
    return ResponseDTO.newResponse(
        connectorService.getConnectorStatistics(accountIdentifier, orgIdentifier, projectIdentifier));
  }

  @POST
  @Path("/listbyfqn")
  @ApiOperation(value = "Gets Connector list", nickname = "listConnectorByFQN")
  public ResponseDTO<List<ConnectorResponseDTO>> listConnectorByFQN(
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Body List<String> connectorsFQN) {
    return ResponseDTO.newResponse(connectorService.listbyFQN(accountIdentifier, connectorsFQN));
  }

  @GET
  @Path("{identifier}/validation-params")
  @ApiOperation(hidden = true, value = "Gets connector validation params")
  @InternalApi
  @Produces("application/x-kryo")
  public ResponseDTO<ConnectorValidationParams> getConnectorValidationParams(
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String connectorIdentifier,
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier) {
    return ResponseDTO.newResponse(connectorHeartbeatService.getConnectorValidationParams(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
  }

  // TODO(UTSAV): will be moved to 340-ce-nextgen
  @POST
  @Path("/getceawstemplateurl")
  @ApiOperation(value = "Get CE Aws Connector Template URL Environment Wise", nickname = "getCEAwsTemplate")
  public ResponseDTO<String> getCEAwsTemplate(
      @QueryParam(NGCommonEntityConstants.IS_EVENTS_ENABLED) Boolean eventsEnabled,
      @QueryParam(NGCommonEntityConstants.IS_CUR_ENABLED) Boolean curEnabled,
      @QueryParam(NGCommonEntityConstants.IS_OPTIMIZATION_ENABLED) Boolean optimizationEnabled) {
    final String templateURL = ceAwsSetupConfig.getTemplateURL();
    return ResponseDTO.newResponse(templateURL);
  }

  @GET
  @Path("/fieldValues")
  @ApiOperation(value = "Get All Allowed field values for Connector Type", nickname = "getAllAllowedFieldValues")
  public ResponseDTO<FieldValues> getAllAllowedFieldValues(
      @NotNull @QueryParam(NGCommonEntityConstants.CONNECTOR_TYPE) ConnectorType connectorType) {
    return ResponseDTO.newResponse(ConnectorAllowedFieldValues.TYPE_TO_FIELDS.get(connectorType));
  }
}