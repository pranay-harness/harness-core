package io.harness.ng.core.remote;

import static io.harness.ng.core.remote.OrganizationMapper.applyUpdateToOrganization;
import static io.harness.ng.core.remote.OrganizationMapper.toOrganization;
import static io.harness.ng.core.remote.OrganizationMapper.writeDto;

import com.google.inject.Inject;

import io.harness.ng.core.RestQueryFilterParser;
import io.harness.ng.core.dto.CreateOrganizationDTO;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.UpdateOrganizationDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.services.api.OrganizationService;
import io.harness.ng.core.utils.PageUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
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
import javax.ws.rs.core.MediaType;

@Api("/organizations")
@Path("/organizations")
@Produces(MediaType.APPLICATION_JSON)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class OrganizationResource {
  private final OrganizationService organizationService;
  private final RestQueryFilterParser restQueryFilterParser;

  @POST
  @ApiOperation(value = "Create an Organization", nickname = "postOrganization")
  public OrganizationDTO create(@NotNull @Valid CreateOrganizationDTO request) {
    Organization organization = organizationService.create(toOrganization(request));
    return writeDto(organization);
  }

  @GET
  @Path("{organizationId}")
  @ApiOperation(value = "Get an Organization", nickname = "getOrganization")
  public Optional<OrganizationDTO> get(@PathParam("organizationId") @NotEmpty String organizationId) {
    Optional<Organization> organizationOptional = organizationService.get(organizationId);
    return organizationOptional.map(OrganizationMapper::writeDto);
  }

  @GET
  @ApiOperation(value = "Get Organization list", nickname = "getOrganizationList")
  public Page<OrganizationDTO> list(@QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("filter") String filter, @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("100") int size, @QueryParam("sort") @DefaultValue("[]") List<String> sort) {
    Criteria criteria = restQueryFilterParser.getCriteriaFromFilterQuery(filter, Organization.class);
    Page<Organization> organizations =
        organizationService.list(accountId, criteria, PageUtils.getPageRequest(page, size, sort));
    return organizations.map(OrganizationMapper::writeDto);
  }

  @PUT
  @Path("{organizationId}")
  @ApiOperation(value = "Update Organization by id", nickname = "putOrganization")
  public Optional<OrganizationDTO> update(@PathParam("organizationId") @NotEmpty String organizationId,
      @NotNull @Valid UpdateOrganizationDTO updateOrganizationDTO) {
    Optional<Organization> organizationOptional = organizationService.get(organizationId);
    if (organizationOptional.isPresent()) {
      Organization organization = organizationOptional.get();
      Organization updatedOrganization =
          organizationService.update(applyUpdateToOrganization(organization, updateOrganizationDTO));
      return Optional.ofNullable(writeDto(updatedOrganization));
    }
    return Optional.empty();
  }

  @DELETE
  @Path("{organizationId}")
  @ApiOperation(value = "Delete Organization by id", nickname = "deleteOrganization")
  @Consumes(MediaType.TEXT_HTML)
  public boolean delete(@PathParam("organizationId") String organizationId) {
    return organizationService.delete(organizationId);
  }
}
