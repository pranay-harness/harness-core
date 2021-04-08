package io.harness.ng.core.remote;

import static io.harness.NGConstants.DEFAULT_ORG_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.accesscontrol.PlatformPermissions.CREATE_PROJECT_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformPermissions.DELETE_PROJECT_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformPermissions.EDIT_PROJECT_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_PROJECT_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.ORGANIZATION;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.PROJECT;
import static io.harness.ng.core.remote.ProjectMapper.toResponseWrapper;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.ModuleType;
import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.dto.ProjectRequest;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
@Api("projects")
@Path("projects")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class ProjectResource {
  private final ProjectService projectService;
  private final OrganizationService organizationService;
  private final AccessControlClient accessControlClient;

  @POST
  @ApiOperation(value = "Create a Project", nickname = "postProject")
  @NGAccessControlCheck(resourceType = ORGANIZATION, permission = CREATE_PROJECT_PERMISSION)
  public ResponseDTO<ProjectResponse> create(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @DefaultValue(DEFAULT_ORG_IDENTIFIER)
      @ResourceIdentifier String orgIdentifier, @NotNull @Valid ProjectRequest projectDTO) {
    Project createdProject = projectService.create(accountIdentifier, orgIdentifier, projectDTO.getProject());
    return ResponseDTO.newResponse(createdProject.getVersion().toString(), toResponseWrapper(createdProject));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Gets a Project by identifier", nickname = "getProject")
  @NGAccessControlCheck(resourceType = PROJECT, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<ProjectResponse> get(
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @DefaultValue(
          DEFAULT_ORG_IDENTIFIER) @OrgIdentifier String orgIdentifier) {
    Optional<Project> projectOptional = projectService.get(accountIdentifier, orgIdentifier, identifier);
    if (!projectOptional.isPresent()) {
      throw new NotFoundException(
          String.format("Project with orgIdentifier [%s] and identifier [%s] not found", orgIdentifier, identifier));
    }
    return ResponseDTO.newResponse(
        projectOptional.get().getVersion().toString(), toResponseWrapper(projectOptional.get()));
  }

  @GET
  @ApiOperation(value = "Get Project list", nickname = "getProjectList")
  public ResponseDTO<PageResponse<ProjectResponse>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam("hasModule") @DefaultValue("true") boolean hasModule,
      @QueryParam(NGResourceFilterConstants.IDENTIFIERS) List<String> identifiers,
      @QueryParam(NGResourceFilterConstants.MODULE_TYPE_KEY) ModuleType moduleType,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm, @BeanParam PageRequest pageRequest) {
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order =
          SortOrder.Builder.aSortOrder().withField(ProjectKeys.lastModifiedAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    Set<String> permittedOrgIds = getPermittedOrganizations(accountIdentifier, orgIdentifier);
    ProjectFilterDTO projectFilterDTO = ProjectFilterDTO.builder()
                                            .searchTerm(searchTerm)
                                            .orgIdentifiers(permittedOrgIds)
                                            .hasModule(hasModule)
                                            .moduleType(moduleType)
                                            .identifiers(identifiers)
                                            .build();
    Page<ProjectResponse> projects =
        projectService.list(accountIdentifier, getPageRequest(pageRequest), projectFilterDTO)
            .map(ProjectMapper::toResponseWrapper);
    return ResponseDTO.newResponse(getNGPageResponse(projects));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update a project by identifier", nickname = "putProject")
  @NGAccessControlCheck(resourceType = PROJECT, permission = EDIT_PROJECT_PERMISSION)
  public ResponseDTO<ProjectResponse> update(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @DefaultValue(DEFAULT_ORG_IDENTIFIER)
      @OrgIdentifier String orgIdentifier, @NotNull @Valid ProjectRequest projectDTO) {
    projectDTO.getProject().setVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);
    Project updatedProject =
        projectService.update(accountIdentifier, orgIdentifier, identifier, projectDTO.getProject());
    return ResponseDTO.newResponse(updatedProject.getVersion().toString(), toResponseWrapper(updatedProject));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a project by identifier", nickname = "deleteProject")
  @NGAccessControlCheck(resourceType = PROJECT, permission = DELETE_PROJECT_PERMISSION)
  public ResponseDTO<Boolean> delete(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @DefaultValue(
          DEFAULT_ORG_IDENTIFIER) @OrgIdentifier String orgIdentifier) {
    return ResponseDTO.newResponse(projectService.delete(
        accountIdentifier, orgIdentifier, identifier, isNumeric(ifMatch) ? parseLong(ifMatch) : null));
  }

  private Set<String> getPermittedOrganizations(@NotNull String accountIdentifier, String orgIdentifier) {
    Set<String> orgIdentifiers;
    if (isEmpty(orgIdentifier)) {
      Criteria orgCriteria = Criteria.where(OrganizationKeys.accountIdentifier)
                                 .is(accountIdentifier)
                                 .and(OrganizationKeys.deleted)
                                 .ne(Boolean.TRUE);
      List<Organization> organizations = organizationService.list(orgCriteria);
      orgIdentifiers = organizations.stream().map(Organization::getIdentifier).collect(Collectors.toSet());
    } else {
      orgIdentifiers = Collections.singleton(orgIdentifier);
    }

    ResourceScope resourceScope = ResourceScope.builder().accountIdentifier(accountIdentifier).build();
    List<PermissionCheckDTO> permissionChecks = orgIdentifiers.stream()
                                                    .map(oi
                                                        -> PermissionCheckDTO.builder()
                                                               .permission(VIEW_PROJECT_PERMISSION)
                                                               .resourceIdentifier(oi)
                                                               .resourceScope(resourceScope)
                                                               .resourceType(ORGANIZATION)
                                                               .build())
                                                    .collect(Collectors.toList());
    AccessCheckResponseDTO accessCheckResponse = accessControlClient.checkForAccess(permissionChecks);
    return accessCheckResponse.getAccessControlList()
        .stream()
        .filter(AccessControlDTO::isPermitted)
        .map(AccessControlDTO::getResourceIdentifier)
        .collect(Collectors.toSet());
  }
}
