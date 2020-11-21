package io.harness.ng.core.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.core.remote.OrganizationMapper.writeDto;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.services.OrganizationService;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
  public ResponseDTO<OrganizationDTO> create(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @Valid OrganizationDTO organizationDTO) {
    Organization updatedOrganization = organizationService.create(accountIdentifier, organizationDTO);
    return ResponseDTO.newResponse(updatedOrganization.getVersion().toString(), writeDto(updatedOrganization));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get an Organization by identifier", nickname = "getOrganization")
  public ResponseDTO<OrganizationDTO> get(@NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    Optional<Organization> organizationOptional = organizationService.get(accountIdentifier, identifier);
    if (!organizationOptional.isPresent()) {
      throw new NotFoundException("Resource not found");
    }
    return ResponseDTO.newResponse(
        organizationOptional.get().getVersion().toString(), writeDto(organizationOptional.get()));
  }

  @GET
  @ApiOperation(value = "Get Organization list", nickname = "getOrganizationList")
  public ResponseDTO<PageResponse<OrganizationDTO>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm, @BeanParam PageRequest pageRequest) {
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order = SortOrder.Builder.aSortOrder().withField("lastModifiedAt", SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    OrganizationFilterDTO organizationFilterDTO = OrganizationFilterDTO.builder().searchTerm(searchTerm).build();
    Page<OrganizationDTO> organizations =
        organizationService.list(accountIdentifier, getPageRequest(pageRequest), organizationFilterDTO)
            .map(OrganizationMapper::writeDto);
    return ResponseDTO.newResponse(getNGPageResponse(organizations));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update an Organization by identifier", nickname = "putOrganization")
  public ResponseDTO<OrganizationDTO> update(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @Valid OrganizationDTO organizationDTO) {
    organizationDTO.setVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);
    Organization updatedOrganization = organizationService.update(accountIdentifier, identifier, organizationDTO);
    return ResponseDTO.newResponse(updatedOrganization.getVersion().toString(), writeDto(updatedOrganization));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete an Organization by identifier", nickname = "deleteOrganization")
  public ResponseDTO<Boolean> delete(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    return ResponseDTO.newResponse(
        organizationService.delete(accountIdentifier, identifier, isNumeric(ifMatch) ? parseLong(ifMatch) : null));
  }
}
