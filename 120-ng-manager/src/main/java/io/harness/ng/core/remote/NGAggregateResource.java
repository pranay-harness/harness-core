package io.harness.ng.core.remote;

import static io.harness.NGConstants.DEFAULT_ORG_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.ModuleType;
import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.api.AggregateOrganizationService;
import io.harness.ng.core.api.AggregateProjectService;
import io.harness.ng.core.api.AggregateUserGroupService;
import io.harness.ng.core.dto.AggregateACLRequest;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.OrganizationAggregateDTO;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.dto.ProjectAggregateDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.UserGroupAggregateDTO;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.HashSet;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import retrofit2.http.Body;

@OwnedBy(PL)
@Api("aggregate")
@Path("aggregate")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class NGAggregateResource {
  private final AggregateOrganizationService aggregateOrganizationService;
  private final AggregateProjectService aggregateProjectService;
  private final AggregateUserGroupService aggregateUserGroupService;

  @GET
  @Path("projects/{identifier}")
  @ApiOperation(value = "Gets a ProjectAggregateDTO by identifier", nickname = "getProjectAggregateDTO")
  public ResponseDTO<ProjectAggregateDTO> get(
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @DefaultValue(DEFAULT_ORG_IDENTIFIER) String orgIdentifier) {
    return ResponseDTO.newResponse(
        aggregateProjectService.getProjectAggregateDTO(accountIdentifier, orgIdentifier, identifier));
  }

  @GET
  @Path("projects")
  @ApiOperation(value = "Get ProjectAggregateDTO list", nickname = "getProjectAggregateDTOList")
  public ResponseDTO<PageResponse<ProjectAggregateDTO>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam("hasModule") @DefaultValue("true") boolean hasModule,
      @QueryParam(NGResourceFilterConstants.MODULE_TYPE_KEY) ModuleType moduleType,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm, @BeanParam PageRequest pageRequest) {
    if (hasNone(pageRequest.getSortOrders())) {
      SortOrder order =
          SortOrder.Builder.aSortOrder().withField(ProjectKeys.lastModifiedAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    ProjectFilterDTO projectFilterDTO = getProjectFilterDTO(searchTerm, orgIdentifier, hasModule, moduleType);
    return ResponseDTO.newResponse(getNGPageResponse(aggregateProjectService.listProjectAggregateDTO(
        accountIdentifier, getPageRequest(pageRequest), projectFilterDTO)));
  }

  private ProjectFilterDTO getProjectFilterDTO(
      String searchTerm, String orgIdentifier, boolean hasModule, ModuleType moduleType) {
    return ProjectFilterDTO.builder()
        .searchTerm(searchTerm)
        .orgIdentifier(orgIdentifier)
        .hasModule(hasModule)
        .moduleType(moduleType)
        .build();
  }

  @GET
  @Path("organizations/{identifier}")
  @ApiOperation(value = "Gets an OrganizationAggregateDTO by identifier", nickname = "getOrganizationAggregateDTO")
  public ResponseDTO<OrganizationAggregateDTO> get(
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    return ResponseDTO.newResponse(
        aggregateOrganizationService.getOrganizationAggregateDTO(accountIdentifier, identifier));
  }

  @GET
  @Path("organizations")
  @ApiOperation(value = "Get OrganizationAggregateDTO list", nickname = "getOrganizationAggregateDTOList")
  public ResponseDTO<PageResponse<OrganizationAggregateDTO>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm, @BeanParam PageRequest pageRequest) {
    OrganizationFilterDTO organizationFilterDTO = OrganizationFilterDTO.builder().searchTerm(searchTerm).build();
    if (hasNone(pageRequest.getSortOrders())) {
      SortOrder harnessManagedOrder =
          SortOrder.Builder.aSortOrder().withField(OrganizationKeys.harnessManaged, SortOrder.OrderType.DESC).build();
      SortOrder nameOrder =
          SortOrder.Builder.aSortOrder().withField(OrganizationKeys.name, SortOrder.OrderType.ASC).build();
      organizationFilterDTO.setIgnoreCase(true);
      pageRequest.setSortOrders(ImmutableList.of(harnessManagedOrder, nameOrder));
    }
    return ResponseDTO.newResponse(getNGPageResponse(aggregateOrganizationService.listOrganizationAggregateDTO(
        accountIdentifier, getPageRequest(pageRequest), organizationFilterDTO)));
  }

  @POST
  @Path("acl/usergroups")
  @ApiOperation(value = "Get Aggregated User Group list", nickname = "getUserGroupAggregateDTOList")
  public ResponseDTO<PageResponse<UserGroupAggregateDTO>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @BeanParam PageRequest pageRequest,
      @Body AggregateACLRequest aggregateACLRequest) {
    if (hasNone(pageRequest.getSortOrders())) {
      SortOrder order =
          SortOrder.Builder.aSortOrder().withField(ProjectKeys.lastModifiedAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    if (aggregateACLRequest == null) {
      aggregateACLRequest =
          AggregateACLRequest.builder().resourceGroupFilter(new HashSet<>()).roleFilter(new HashSet<>()).build();
    }
    return ResponseDTO.newResponse(aggregateUserGroupService.listAggregateUserGroups(
        pageRequest, accountIdentifier, orgIdentifier, projectIdentifier, aggregateACLRequest));
  }
}
