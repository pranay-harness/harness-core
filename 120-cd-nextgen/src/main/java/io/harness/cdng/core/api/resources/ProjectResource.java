package io.harness.cdng.core.api.resources;

import com.google.inject.Inject;

import io.harness.cdng.core.entities.Project;
import io.harness.cdng.core.services.api.ProjectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api("/projects")
@Path("/projects")
@Produces(MediaType.APPLICATION_JSON)
public class ProjectResource {
  private final ProjectService projectService;

  @Inject
  public ProjectResource(ProjectService projectService) {
    this.projectService = projectService;
  }

  @POST
  @ApiOperation("Creates a new Project")
  public Project save(Project project) {
    return projectService.save(project);
  }

  @GET
  @Path("{projectId}")
  @ApiOperation("Gets a Project by project id")
  public Project get(@PathParam("projectId") @NotEmpty String projectId) {
    return projectService.get(projectId);
  }
}