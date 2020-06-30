package io.harness.ng.core.remote;

import static io.harness.ng.core.remote.OrganizationMapper.applyUpdateToOrganization;
import static io.harness.ng.core.remote.OrganizationMapper.toOrganization;
import static io.harness.ng.core.remote.OrganizationMapper.writeDto;

import com.google.inject.Inject;

import io.harness.ng.core.ErrorDTO;
import io.harness.ng.core.FailureDTO;
import io.harness.ng.core.ResponseDTO;
import io.harness.ng.core.dto.CreateOrganizationDTO;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.UpdateOrganizationDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.services.api.OrganizationService;
import io.harness.utils.PageUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("organizations")
@Path("accounts/{accountIdentifier}/organizations")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class OrganizationResource {
  private final OrganizationService organizationService;

  @POST
  @ApiOperation(value = "Create an Organization", nickname = "postOrganization")
  public ResponseDTO<OrganizationDTO> create(
      @PathParam("accountIdentifier") String accountIdentifier, @NotNull @Valid CreateOrganizationDTO request) {
    Organization organization = toOrganization(request);
    organization.setAccountIdentifier(accountIdentifier);
    Organization updatedOrganization = organizationService.create(organization);
    return ResponseDTO.newResponse(writeDto(updatedOrganization));
  }

  @GET
  @Path("{organizationIdentifier}")
  @ApiOperation(value = "Get an Organization", nickname = "getOrganization")
  public ResponseDTO<Optional<OrganizationDTO>> get(@PathParam("accountIdentifier") String accountIdentifier,
      @PathParam("organizationIdentifier") String organizationIdentifier) {
    Optional<Organization> organizationOptional = organizationService.get(accountIdentifier, organizationIdentifier);
    return ResponseDTO.newResponse(organizationOptional.map(OrganizationMapper::writeDto));
  }

  @GET
  @ApiOperation(value = "Get Organization list", nickname = "getOrganizationList")
  public ResponseDTO<Page<OrganizationDTO>> list(@PathParam("accountIdentifier") String accountIdentifier,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("100") int size,
      @QueryParam("sort") @DefaultValue("[]") List<String> sort) {
    Criteria criteria = Criteria.where(OrganizationKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(OrganizationKeys.deleted)
                            .is(false);
    Page<Organization> organizations = organizationService.list(criteria, PageUtils.getPageRequest(page, size, sort));
    return ResponseDTO.newResponse(organizations.map(OrganizationMapper::writeDto));
  }

  @PUT
  @Path("{organizationIdentifier}")
  @ApiOperation(value = "Update Organization by identifier", nickname = "putOrganization")
  public ResponseDTO<Optional<OrganizationDTO>> update(@PathParam("accountIdentifier") String accountIdentifier,
      @PathParam("organizationIdentifier") String orgIdentifier,
      @NotNull @Valid UpdateOrganizationDTO updateOrganizationDTO) {
    Optional<Organization> organizationOptional = organizationService.get(accountIdentifier, orgIdentifier);
    if (organizationOptional.isPresent()) {
      Organization organization = organizationOptional.get();
      Organization updatedOrganization =
          organizationService.update(applyUpdateToOrganization(organization, updateOrganizationDTO));
      return ResponseDTO.newResponse(Optional.ofNullable(writeDto(updatedOrganization)));
    }
    return ResponseDTO.newResponse(Optional.empty());
  }

  @DELETE
  @Path("{organizationIdentifier}")
  @ApiOperation(value = "Delete Organization by identifier", nickname = "deleteOrganization")
  public ResponseDTO<Boolean> delete(@PathParam("accountIdentifier") String accountIdentifier,
      @PathParam("organizationIdentifier") String organizationIdentifier) {
    return ResponseDTO.newResponse(organizationService.delete(accountIdentifier, organizationIdentifier));
  }
}
