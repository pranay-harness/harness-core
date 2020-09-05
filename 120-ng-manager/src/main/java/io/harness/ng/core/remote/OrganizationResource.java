package io.harness.ng.core.remote;

import static io.harness.ng.NGConstants.ACCOUNT_KEY;
import static io.harness.ng.NGConstants.IDENTIFIER_KEY;
import static io.harness.ng.NGConstants.PAGE_KEY;
import static io.harness.ng.NGConstants.SEARCH_TERM_KEY;
import static io.harness.ng.NGConstants.SIZE_KEY;
import static io.harness.ng.NGConstants.SORT_KEY;
import static io.harness.ng.core.remote.OrganizationMapper.writeDto;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import com.google.inject.Inject;

import io.harness.beans.NGPageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.services.OrganizationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;

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
@Path("organizations")
@Produces({"application/json", "text/yaml"})
@Consumes({"application/json", "text/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
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
      @NotNull @QueryParam(ACCOUNT_KEY) String accountIdentifier, @NotNull @Valid OrganizationDTO organizationDTO) {
    Organization updatedOrganization = organizationService.create(accountIdentifier, organizationDTO);
    return ResponseDTO.newResponse(writeDto(updatedOrganization));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get an Organization by identifier", nickname = "getOrganization")
  public ResponseDTO<Optional<OrganizationDTO>> get(@NotNull @PathParam(IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(ACCOUNT_KEY) String accountIdentifier) {
    Optional<Organization> organizationOptional = organizationService.get(accountIdentifier, identifier);
    return ResponseDTO.newResponse(organizationOptional.map(OrganizationMapper::writeDto));
  }

  @GET
  @ApiOperation(value = "Get Organization list", nickname = "getOrganizationList")
  public ResponseDTO<NGPageResponse<OrganizationDTO>> list(@NotNull @QueryParam(ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(SEARCH_TERM_KEY) String searchTerm, @QueryParam(PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(SIZE_KEY) @DefaultValue("100") int size,
      @QueryParam(SORT_KEY) @DefaultValue("[]") List<String> sort) {
    OrganizationFilterDTO organizationFilterDTO = OrganizationFilterDTO.builder().searchTerm(searchTerm).build();
    Page<OrganizationDTO> organizations =
        organizationService.list(accountIdentifier, getPageRequest(page, size, sort), organizationFilterDTO)
            .map(OrganizationMapper::writeDto);
    return ResponseDTO.newResponse(getNGPageResponse(organizations));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update an Organization by identifier", nickname = "putOrganization")
  public ResponseDTO<OrganizationDTO> update(@NotNull @PathParam(IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(ACCOUNT_KEY) String accountIdentifier, @NotNull @Valid OrganizationDTO organizationDTO) {
    Organization updatedOrganization = organizationService.update(accountIdentifier, identifier, organizationDTO);
    return ResponseDTO.newResponse(writeDto(updatedOrganization));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete an Organization by identifier", nickname = "deleteOrganization")
  public ResponseDTO<Boolean> delete(@NotNull @PathParam(IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(ACCOUNT_KEY) String accountIdentifier) {
    return ResponseDTO.newResponse(organizationService.delete(accountIdentifier, identifier));
  }
}
