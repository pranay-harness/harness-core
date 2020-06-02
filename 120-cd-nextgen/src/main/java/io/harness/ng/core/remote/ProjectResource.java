package io.harness.ng.core.remote;

import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import io.harness.ng.core.dto.CreateProjectRequest;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.UpdateProjectRequest;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.services.api.ProjectService;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api("/projects")
@Path("/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectResource {
  private final ProjectService projectService;
  private final ProjectMapper projectMapper;

  @Inject
  ProjectResource(ProjectService projectService, ProjectMapper projectMapper) {
    this.projectService = projectService;
    this.projectMapper = projectMapper;
  }

  @POST
  public ProjectDTO create(@NotNull @Valid CreateProjectRequest createProjectRequest) {
    Project project = projectService.create(projectMapper.toProject(createProjectRequest));
    return projectMapper.writeDTO(project);
  }

  @GET
  @Path("{projectId}")
  public Optional<ProjectDTO> get(@PathParam("projectId") @NotEmpty String projectId) {
    Optional<Project> project = projectService.get(projectId);
    return project.map(projectMapper::writeDTO);
  }

  @GET
  public List<ProjectDTO> list() {
    List<Project> projects = projectService.getAll();
    return projects.stream().map(projectMapper::writeDTO).collect(toList());
  }

  @PUT
  @Path("{projectId}")
  public Optional<ProjectDTO> update(
      @PathParam("projectId") @NotEmpty String projectId, @NotNull @Valid UpdateProjectRequest updateProjectRequest) {
    Optional<Project> project = projectService.get(projectId);
    if (project.isPresent()) {
      Project updatedProject =
          projectService.update(projectMapper.applyUpdateToProject(project.get(), updateProjectRequest));
      return Optional.ofNullable(projectMapper.writeDTO(updatedProject));
    }
    return Optional.empty();
  }
}