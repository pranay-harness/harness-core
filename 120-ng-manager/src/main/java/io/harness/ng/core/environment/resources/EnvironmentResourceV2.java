package io.harness.ng.core.environment.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_PROJECT_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.PROJECT;
import static io.harness.pms.rbac.NGResourceType.ENVIRONMENT;
import static io.harness.rbac.CDNGRbacPermissions.ENVIRONMENT_CREATE_PERMISSION;
import static io.harness.rbac.CDNGRbacPermissions.ENVIRONMENT_UPDATE_PERMISSION;
import static io.harness.rbac.CDNGRbacPermissions.ENVIRONMENT_VIEW_PERMISSION;
import static io.harness.utils.PageUtils.getNGPageResponse;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.Environment.EnvironmentKeys;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.environment.dto.EnvironmentRequestDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponse;
import io.harness.ng.core.environment.mappers.EnvironmentMapper;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.ng.core.utils.CoreCriteriaUtils;
import io.harness.rbac.CDNGRbacUtility;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

@NextGenManagerAuth
@Api("/environmentsV2")
@Path("/environmentsV2")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@OwnedBy(HarnessTeam.PIPELINE)
public class EnvironmentResourceV2 {
  private final EnvironmentService environmentService;
  private final AccessControlClient accessControlClient;

  @GET
  @Path("{environmentIdentifier}")
  @NGAccessControlCheck(resourceType = ENVIRONMENT, permission = "core_environment_view")
  @ApiOperation(value = "Gets a Environment by identifier", nickname = "getEnvironmentV2")
  public ResponseDTO<EnvironmentResponse> get(
      @PathParam("environmentIdentifier") @ResourceIdentifier String environmentIdentifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.DELETED_KEY) @DefaultValue("false") boolean deleted) {
    Optional<Environment> environment =
        environmentService.get(accountId, orgIdentifier, projectIdentifier, environmentIdentifier, deleted);
    String version = "0";
    if (environment.isPresent()) {
      version = environment.get().getVersion().toString();
    }
    return ResponseDTO.newResponse(version, environment.map(EnvironmentMapper::toResponseWrapper).orElse(null));
  }

  @POST
  @ApiOperation(value = "Create an Environment", nickname = "createEnvironmentV2")
  public ResponseDTO<EnvironmentResponse> create(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @Valid EnvironmentRequestDTO environmentRequestDTO) {
    throwExceptionForNoRequestDTO(environmentRequestDTO);
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, environmentRequestDTO.getOrgIdentifier(),
                                                  environmentRequestDTO.getProjectIdentifier()),
        Resource.of(ENVIRONMENT, null), ENVIRONMENT_CREATE_PERMISSION);
    if (environmentRequestDTO.getType() == null) {
      throw new InvalidRequestException(
          "Type for an environment cannot be empty. Possible values: " + Arrays.toString(EnvironmentType.values()));
    }
    Environment environmentEntity = EnvironmentMapper.toEnvironmentEntity(accountId, environmentRequestDTO);
    Environment createdEnvironment = environmentService.create(environmentEntity);
    return ResponseDTO.newResponse(
        createdEnvironment.getVersion().toString(), EnvironmentMapper.toResponseWrapper(createdEnvironment));
  }

  @DELETE
  @Path("{environmentIdentifier}")
  @ApiOperation(value = "Delete en environment by identifier", nickname = "deleteEnvironmentV2")
  @NGAccessControlCheck(resourceType = ENVIRONMENT, permission = "core_environment_delete")
  public ResponseDTO<Boolean> delete(@HeaderParam(IF_MATCH) String ifMatch,
      @PathParam("environmentIdentifier") @ResourceIdentifier String environmentIdentifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier) {
    return ResponseDTO.newResponse(environmentService.delete(accountId, orgIdentifier, projectIdentifier,
        environmentIdentifier, isNumeric(ifMatch) ? parseLong(ifMatch) : null));
  }

  @PUT
  @ApiOperation(value = "Update an environment by identifier", nickname = "updateEnvironmentV2")
  public ResponseDTO<EnvironmentResponse> update(@HeaderParam(IF_MATCH) String ifMatch,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @Valid EnvironmentRequestDTO environmentRequestDTO) {
    throwExceptionForNoRequestDTO(environmentRequestDTO);
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, environmentRequestDTO.getOrgIdentifier(),
                                                  environmentRequestDTO.getProjectIdentifier()),
        Resource.of(ENVIRONMENT, environmentRequestDTO.getIdentifier()), ENVIRONMENT_UPDATE_PERMISSION);

    Environment requestEnvironment = EnvironmentMapper.toEnvironmentEntity(accountId, environmentRequestDTO);
    requestEnvironment.setVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);
    Environment updatedEnvironment = environmentService.update(requestEnvironment);
    return ResponseDTO.newResponse(
        updatedEnvironment.getVersion().toString(), EnvironmentMapper.toResponseWrapper(updatedEnvironment));
  }

  @PUT
  @Path("upsert")
  @ApiOperation(value = "Upsert an environment by identifier", nickname = "upsertEnvironmentV2")
  public ResponseDTO<EnvironmentResponse> upsert(@HeaderParam(IF_MATCH) String ifMatch,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @Valid EnvironmentRequestDTO environmentRequestDTO) {
    throwExceptionForNoRequestDTO(environmentRequestDTO);
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, environmentRequestDTO.getOrgIdentifier(),
                                                  environmentRequestDTO.getProjectIdentifier()),
        Resource.of(ENVIRONMENT, environmentRequestDTO.getIdentifier()), ENVIRONMENT_UPDATE_PERMISSION);

    Environment requestEnvironment = EnvironmentMapper.toEnvironmentEntity(accountId, environmentRequestDTO);
    requestEnvironment.setVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);
    Environment upsertedEnvironment = environmentService.upsert(requestEnvironment);
    return ResponseDTO.newResponse(
        upsertedEnvironment.getVersion().toString(), EnvironmentMapper.toResponseWrapper(upsertedEnvironment));
  }

  @GET
  @ApiOperation(value = "Gets environment list", nickname = "getEnvironmentList")
  public ResponseDTO<PageResponse<EnvironmentResponse>> listEnvironment(@QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("100") int size,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam("envIdentifiers") List<String> envIdentifiers, @QueryParam("sort") List<String> sort) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgIdentifier, projectIdentifier),
        Resource.of(ENVIRONMENT, null), ENVIRONMENT_VIEW_PERMISSION, "Unauthorized to list environments");
    Criteria criteria = CoreCriteriaUtils.createCriteriaForGetList(accountId, orgIdentifier, projectIdentifier, false);
    Pageable pageRequest;

    if (isNotEmpty(envIdentifiers)) {
      criteria.and(EnvironmentKeys.identifier).in(envIdentifiers);
    }
    if (isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, EnvironmentKeys.createdAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }
    Page<EnvironmentResponse> environmentList =
        environmentService.list(criteria, pageRequest).map(EnvironmentMapper::toResponseWrapper);
    return ResponseDTO.newResponse(getNGPageResponse(environmentList));
  }

  @GET
  @Path("/list/access")
  @ApiOperation(value = "Gets environment access list", nickname = "getEnvironmentAccessList")
  public ResponseDTO<List<EnvironmentResponse>> listAccessEnvironment(@QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("100") int size,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam("envIdentifiers") List<String> envIdentifiers, @QueryParam("sort") List<String> sort) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgIdentifier, projectIdentifier),
        Resource.of(PROJECT, projectIdentifier), VIEW_PROJECT_PERMISSION, "Unauthorized to list environments");
    Criteria criteria = CoreCriteriaUtils.createCriteriaForGetList(accountId, orgIdentifier, projectIdentifier, false);

    if (isNotEmpty(envIdentifiers)) {
      criteria.and(EnvironmentKeys.identifier).in(envIdentifiers);
    }

    List<EnvironmentResponse> environmentList = environmentService.listAccess(criteria)
                                                    .stream()
                                                    .map(EnvironmentMapper::toResponseWrapper)
                                                    .collect(Collectors.toList());

    List<PermissionCheckDTO> permissionCheckDTOS = environmentList.stream()
                                                       .map(CDNGRbacUtility::environmentResponseToPermissionCheckDTO)
                                                       .collect(Collectors.toList());
    List<AccessControlDTO> accessControlList =
        accessControlClient.checkForAccess(permissionCheckDTOS).getAccessControlList();
    return ResponseDTO.newResponse(filterEnvironmentResponseByPermissionAndId(accessControlList, environmentList));
  }

  private List<EnvironmentResponse> filterEnvironmentResponseByPermissionAndId(
      List<AccessControlDTO> accessControlList, List<EnvironmentResponse> environmentList) {
    List<EnvironmentResponse> filteredAccessControlDtoList = new ArrayList<>();
    for (int i = 0; i < accessControlList.size(); i++) {
      AccessControlDTO accessControlDTO = accessControlList.get(i);
      EnvironmentResponse environmentResponse = environmentList.get(i);
      if (accessControlDTO.isPermitted()
          && environmentResponse.getEnvironment().getIdentifier().equals(accessControlDTO.getResourceIdentifier())) {
        filteredAccessControlDtoList.add(environmentResponse);
      }
    }
    return filteredAccessControlDtoList;
  }

  private void throwExceptionForNoRequestDTO(EnvironmentRequestDTO dto) {
    if (dto == null) {
      throw new InvalidRequestException(
          "No request body sent in the API. Following field is required: identifier, type. Other optional fields: name, orgIdentifier, projectIdentifier, tags, description, version");
    }
  }
}
