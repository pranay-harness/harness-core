package io.harness.ng.core.remote;

import static io.harness.NGConstants.ACCOUNT_KEY;
import static io.harness.NGConstants.IDENTIFIER_KEY;
import static io.harness.NGConstants.MODULE_TYPE_KEY;
import static io.harness.NGConstants.ORG_KEY;
import static io.harness.NGConstants.SEARCH_TERM_KEY;
import static io.harness.ng.core.remote.ProjectMapper.writeDTO;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import com.google.inject.Inject;

import io.harness.ModuleType;
import io.harness.beans.NGPageRequest;
import io.harness.beans.NGPageResponse;
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
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("projects")
@Path("projects")
@Produces({"application/json", "text/yaml"})
@Consumes({"application/json", "text/yaml"})
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
  public ResponseDTO<ProjectDTO> create(@NotNull @QueryParam(ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(ORG_KEY) String orgIdentifier, @NotNull @Valid ProjectDTO projectDTO) {
    Project createdProject = projectService.create(accountIdentifier, orgIdentifier, projectDTO);
    return ResponseDTO.newResponse(writeDTO(createdProject));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Gets a Project by identifier", nickname = "getProject")
  public ResponseDTO<Optional<ProjectDTO>> get(@NotNull @PathParam(IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(ACCOUNT_KEY) String accountIdentifier, @NotNull @QueryParam(ORG_KEY) String orgIdentifier) {
    Optional<ProjectDTO> projectDTO =
        projectService.get(accountIdentifier, orgIdentifier, identifier).map(ProjectMapper::writeDTO);
    return ResponseDTO.newResponse(projectDTO);
  }

  @GET
  @ApiOperation(value = "Get Project list", nickname = "getProjectList")
  public ResponseDTO<NGPageResponse<ProjectDTO>> list(@NotNull @QueryParam(ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(ORG_KEY) String orgIdentifier, @QueryParam("hasModule") @DefaultValue("true") boolean hasModule,
      @QueryParam(MODULE_TYPE_KEY) ModuleType moduleType, @QueryParam(SEARCH_TERM_KEY) String searchTerm,
      @BeanParam NGPageRequest ngPageRequest) {
    ProjectFilterDTO projectFilterDTO = ProjectFilterDTO.builder()
                                            .searchTerm(searchTerm)
                                            .orgIdentifier(orgIdentifier)
                                            .hasModule(hasModule)
                                            .moduleType(moduleType)
                                            .build();
    Page<ProjectDTO> projects = projectService.list(accountIdentifier, getPageRequest(ngPageRequest), projectFilterDTO)
                                    .map(ProjectMapper::writeDTO);
    return ResponseDTO.newResponse(getNGPageResponse(projects));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update a project by identifier", nickname = "putProject")
  public ResponseDTO<ProjectDTO> update(@NotNull @PathParam(IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(ACCOUNT_KEY) String accountIdentifier, @NotNull @QueryParam(ORG_KEY) String orgIdentifier,
      @NotNull @Valid ProjectDTO projectDTO) {
    Project updatedProject = projectService.update(accountIdentifier, orgIdentifier, identifier, projectDTO);
    return ResponseDTO.newResponse(writeDTO(updatedProject));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a project by identifier", nickname = "deleteProject")
  public ResponseDTO<Boolean> delete(@NotNull @PathParam(IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(ACCOUNT_KEY) String accountIdentifier, @NotNull @QueryParam(ORG_KEY) String orgIdentifier) {
    return ResponseDTO.newResponse(projectService.delete(accountIdentifier, orgIdentifier, identifier));
  }
}
