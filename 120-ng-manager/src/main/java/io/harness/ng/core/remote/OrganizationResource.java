package io.harness.ng.core.remote;

import com.google.inject.Inject;

import io.harness.ng.core.dto.CreateOrganizationRequest;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.UpdateOrganizationRequest;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.services.api.OrganizationService;
import io.swagger.annotations.Api;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api("/organizations")
@Path("/organizations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class OrganizationResource {
  private final OrganizationService organizationService;
  private final OrganizationMapper organizationMapper;

  @POST
  public OrganizationDTO createOrganization(@NotNull @Valid CreateOrganizationRequest request) {
    Organization organization = organizationService.create(organizationMapper.toOrganization(request));
    return organizationMapper.writeDto(organization);
  }

  @GET
  @Path("{organizationId}")
  public Optional<OrganizationDTO> getOrganization(@PathParam("organizationId") @NotEmpty String organizationId) {
    Optional<Organization> organizationOptional = organizationService.get(organizationId);
    return organizationOptional.map(organizationMapper::writeDto);
  }

  @GET
  public List<OrganizationDTO> getOrganizationsForAccount(@QueryParam("accountId") @NotEmpty String accountId) {
    List<Organization> organizations = organizationService.getAll(accountId);
    return organizations.stream().map(organizationMapper::writeDto).collect(Collectors.toList());
  }

  @PUT
  @Path("{organizationId}")
  public Optional<OrganizationDTO> updateOrganization(
      @PathParam("organizationId") @NotEmpty String organizationId, @NotNull @Valid UpdateOrganizationRequest request) {
    Optional<Organization> organizationOptional = organizationService.get(organizationId);
    organizationOptional.map(organization -> {
      Organization updatedOrganization =
          organizationService.update(organizationMapper.applyUpdateToOrganization(organization, request));
      return Optional.ofNullable(organizationMapper.writeDto(updatedOrganization));
    });
    return Optional.empty();
  }
}
