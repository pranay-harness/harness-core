package io.harness.ng.core.remote;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.core.remote.ProjectMapper.writeDTO;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.ModuleType;
import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.services.ProjectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.Optional;
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
public class ProjectResource {
  private final ProjectService projectService;

  @POST
  @ApiOperation(value = "Create a Project", nickname = "postProject")
  public ResponseDTO<ProjectDTO> create(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @Valid ProjectDTO projectDTO) {
    Project createdProject = projectService.create(accountIdentifier, orgIdentifier, projectDTO);
    return ResponseDTO.newResponse(createdProject.getVersion().toString(), writeDTO(createdProject));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Gets a Project by identifier", nickname = "getProject")
  public ResponseDTO<ProjectDTO> get(@NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier) {
    Optional<Project> projectOptional = projectService.get(accountIdentifier, orgIdentifier, identifier);
    if (!projectOptional.isPresent()) {
      throw new NotFoundException("Resource not found");
    }
    return ResponseDTO.newResponse(projectOptional.get().getVersion().toString(), writeDTO(projectOptional.get()));
  }

  @GET
  @ApiOperation(value = "Get Project list", nickname = "getProjectList")
  public ResponseDTO<PageResponse<ProjectDTO>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam("hasModule") @DefaultValue("true") boolean hasModule,
      @QueryParam(NGResourceFilterConstants.MODULE_TYPE_KEY) ModuleType moduleType,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm, @BeanParam PageRequest pageRequest) {
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order = SortOrder.Builder.aSortOrder().withField("lastModifiedAt", SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    ProjectFilterDTO projectFilterDTO = ProjectFilterDTO.builder()
                                            .searchTerm(searchTerm)
                                            .orgIdentifier(orgIdentifier)
                                            .hasModule(hasModule)
                                            .moduleType(moduleType)
                                            .build();
    Page<ProjectDTO> projects = projectService.list(accountIdentifier, getPageRequest(pageRequest), projectFilterDTO)
                                    .map(ProjectMapper::writeDTO);
    return ResponseDTO.newResponse(getNGPageResponse(projects));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update a project by identifier", nickname = "putProject")
  public ResponseDTO<ProjectDTO> update(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @Valid ProjectDTO projectDTO) {
    projectDTO.setVersion(Optional.ofNullable(ifMatch).map(Long::parseLong).orElse(null));
    Project updatedProject = projectService.update(accountIdentifier, orgIdentifier, identifier, projectDTO);
    return ResponseDTO.newResponse(updatedProject.getVersion().toString(), writeDTO(updatedProject));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a project by identifier", nickname = "deleteProject")
  public ResponseDTO<Boolean> delete(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier) {
    return ResponseDTO.newResponse(projectService.delete(
        accountIdentifier, orgIdentifier, identifier, Optional.ofNullable(ifMatch).map(Long::parseLong).orElse(null)));
  }
}
