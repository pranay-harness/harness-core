package io.harness.ng.core.remote;

import static io.harness.ng.core.remote.ProjectMapper.applyUpdateToProject;
import static io.harness.ng.core.remote.ProjectMapper.toProject;
import static io.harness.ng.core.remote.ProjectMapper.writeDTO;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import com.google.inject.Inject;

import io.harness.beans.NGPageResponse;
import io.harness.ng.core.dto.CreateProjectDTO;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.UpdateProjectDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.services.api.OrganizationService;
import io.harness.ng.core.services.api.ProjectService;
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

@Api("projects")
@Path("organizations/{orgIdentifier}/projects")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class ProjectResource {
  private final ProjectService projectService;
  private final OrganizationService organizationService;

  @POST
  @ApiOperation(value = "Create a Project", nickname = "postProject")
  public ResponseDTO<ProjectDTO> create(
      @PathParam("orgIdentifier") String orgIdentifier, @NotNull @Valid CreateProjectDTO createProjectDTO) {
    Project project = toProject(createProjectDTO);
    project.setOrgIdentifier(orgIdentifier);
    ProjectDTO createdProject = writeDTO(projectService.create(project));
    Optional<Organization> organization = organizationService.get(createdProject.getAccountIdentifier(), orgIdentifier);
    organization.ifPresent(value -> createdProject.setOrganizationName(value.getName()));
    return ResponseDTO.newResponse(createdProject);
  }

  @GET
  @Path("{projectIdentifier}")
  @ApiOperation(value = "Gets a Project by identifier", nickname = "getProject")
  public ResponseDTO<Optional<ProjectDTO>> get(
      @PathParam("orgIdentifier") String orgIdentifier, @PathParam("projectIdentifier") String projectIdentifier) {
    Optional<ProjectDTO> projectDTO = projectService.get(orgIdentifier, projectIdentifier).map(ProjectMapper::writeDTO);
    if (projectDTO.isPresent()) {
      Optional<Organization> organization =
          organizationService.get(projectDTO.get().getAccountIdentifier(), orgIdentifier);
      organization.ifPresent(value -> projectDTO.get().setOrganizationName(value.getName()));
    }
    return ResponseDTO.newResponse(projectDTO);
  }

  @GET
  @ApiOperation(value = "Gets Project list for an organization", nickname = "getProjectListForOrganization")
  public ResponseDTO<NGPageResponse<ProjectDTO>> listProjectsForOrganization(
      @PathParam("orgIdentifier") String orgIdentifier, @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("100") int size, @QueryParam("sort") List<String> sort) {
    Criteria criteria = Criteria.where(ProjectKeys.orgIdentifier).is(orgIdentifier).and(ProjectKeys.deleted).ne(true);
    Page<ProjectDTO> projects =
        projectService.list(criteria, getPageRequest(page, size, sort)).map(ProjectMapper::writeDTO);
    if (!projects.getContent().isEmpty()) {
      Optional<Organization> organization =
          organizationService.get(projects.getContent().get(0).getAccountIdentifier(), orgIdentifier);
      projects.getContent().forEach(
          project -> organization.ifPresent(value -> project.setOrganizationName(value.getName())));
    }
    return ResponseDTO.newResponse(getNGPageResponse(projects));
  }

  @PUT
  @Path("{projectIdentifier}")
  @ApiOperation(value = "Update a project by identifier", nickname = "putProject")
  public ResponseDTO<Optional<ProjectDTO>> update(@PathParam("orgIdentifier") String orgIdentifier,
      @PathParam("projectIdentifier") String projectIdentifier, @NotNull @Valid UpdateProjectDTO updateProjectDTO) {
    Optional<Project> projectOptional = projectService.get(orgIdentifier, projectIdentifier);
    if (projectOptional.isPresent()) {
      Project project = applyUpdateToProject(projectOptional.get(), updateProjectDTO);
      ProjectDTO updatedProject = writeDTO(projectService.update(project));
      Optional<Organization> organization =
          organizationService.get(updatedProject.getAccountIdentifier(), orgIdentifier);
      organization.ifPresent(value -> updatedProject.setOrganizationName(value.getName()));
      return ResponseDTO.newResponse(Optional.of(updatedProject));
    }
    return ResponseDTO.newResponse(Optional.empty());
  }

  @DELETE
  @Path("{projectIdentifier}")
  @ApiOperation(value = "Delete a project by identifier", nickname = "deleteProject")
  public ResponseDTO<Boolean> delete(
      @PathParam("orgIdentifier") String orgIdentifier, @PathParam("projectIdentifier") String projectIdentifier) {
    return ResponseDTO.newResponse(projectService.delete(orgIdentifier, projectIdentifier));
  }
}
