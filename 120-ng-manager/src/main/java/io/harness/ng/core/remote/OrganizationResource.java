package io.harness.ng.core.remote;

import static io.harness.NGConstants.DEFAULT_ORG_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.ng.accesscontrol.PlatformPermissions.CREATE_ORGANIZATION_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformPermissions.DELETE_ORGANIZATION_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformPermissions.EDIT_ORGANIZATION_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_ORGANIZATION_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.ORGANIZATION;
import static io.harness.ng.core.remote.OrganizationMapper.toResponseWrapper;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.dto.OrganizationRequest;
import io.harness.ng.core.dto.OrganizationResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.services.OrganizationService;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
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

@OwnedBy(PL)
@Api("organizations")
@Path("organizations")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class OrganizationResource {
  private final OrganizationService organizationService;

  @POST
  @ApiOperation(value = "Create an Organization", nickname = "postOrganization")
  @NGAccessControlCheck(resourceType = ORGANIZATION, permission = CREATE_ORGANIZATION_PERMISSION)
  public ResponseDTO<OrganizationResponse> create(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @Valid OrganizationRequest organizationDTO) {
    if (DEFAULT_ORG_IDENTIFIER.equals(organizationDTO.getOrganization().getIdentifier())) {
      throw new InvalidRequestException(
          String.format("%s cannot be used as org identifier", DEFAULT_ORG_IDENTIFIER), USER);
    }
    Organization updatedOrganization = organizationService.create(accountIdentifier, organizationDTO.getOrganization());
    return ResponseDTO.newResponse(updatedOrganization.getVersion().toString(), toResponseWrapper(updatedOrganization));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get an Organization by identifier", nickname = "getOrganization")
  @NGAccessControlCheck(resourceType = ORGANIZATION, permission = VIEW_ORGANIZATION_PERMISSION)
  public ResponseDTO<OrganizationResponse> get(
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    Optional<Organization> organizationOptional = organizationService.get(accountIdentifier, identifier);
    if (!organizationOptional.isPresent()) {
      throw new NotFoundException(String.format("Organization with identifier [%s] not found", identifier));
    }
    return ResponseDTO.newResponse(
        organizationOptional.get().getVersion().toString(), toResponseWrapper(organizationOptional.get()));
  }

  @GET
  @ApiOperation(value = "Get Organization list", nickname = "getOrganizationList")
  public ResponseDTO<PageResponse<OrganizationResponse>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGResourceFilterConstants.IDENTIFIERS) List<String> identifiers,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm, @BeanParam PageRequest pageRequest) {
    OrganizationFilterDTO organizationFilterDTO =
        OrganizationFilterDTO.builder().searchTerm(searchTerm).identifiers(identifiers).build();
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder harnessManagedOrder =
          SortOrder.Builder.aSortOrder().withField(OrganizationKeys.harnessManaged, SortOrder.OrderType.DESC).build();
      SortOrder nameOrder =
          SortOrder.Builder.aSortOrder().withField(OrganizationKeys.name, SortOrder.OrderType.ASC).build();
      pageRequest.setSortOrders(ImmutableList.of(harnessManagedOrder, nameOrder));
      organizationFilterDTO.setIgnoreCase(true);
    }
    Page<Organization> orgsPage =
        organizationService.listPermittedOrgs(accountIdentifier, getPageRequest(pageRequest), organizationFilterDTO);
    return ResponseDTO.newResponse(getNGPageResponse(orgsPage.map(OrganizationMapper::toResponseWrapper)));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update an Organization by identifier", nickname = "putOrganization")
  @NGAccessControlCheck(resourceType = ORGANIZATION, permission = EDIT_ORGANIZATION_PERMISSION)
  public ResponseDTO<OrganizationResponse> update(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @Valid OrganizationRequest organizationDTO) {
    if (DEFAULT_ORG_IDENTIFIER.equals(identifier)) {
      throw new InvalidRequestException(
          String.format(
              "Update operation not supported for Default Organization (identifier: [%s])", DEFAULT_ORG_IDENTIFIER),
          USER);
    }
    organizationDTO.getOrganization().setVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);
    Organization updatedOrganization =
        organizationService.update(accountIdentifier, identifier, organizationDTO.getOrganization());
    return ResponseDTO.newResponse(updatedOrganization.getVersion().toString(), toResponseWrapper(updatedOrganization));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete an Organization by identifier", nickname = "deleteOrganization")
  @NGAccessControlCheck(resourceType = ORGANIZATION, permission = DELETE_ORGANIZATION_PERMISSION)
  public ResponseDTO<Boolean> delete(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    if (DEFAULT_ORG_IDENTIFIER.equals(identifier)) {
      throw new InvalidRequestException(
          String.format(
              "Delete operation not supported for Default Organization (identifier: [%s])", DEFAULT_ORG_IDENTIFIER),
          USER);
    }
    return ResponseDTO.newResponse(
        organizationService.delete(accountIdentifier, identifier, isNumeric(ifMatch) ? parseLong(ifMatch) : null));
  }
}
