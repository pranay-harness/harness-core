package io.harness.ng.core.remote;

import static io.harness.ng.core.remote.ProjectMapper.applyUpdateToProject;
import static io.harness.ng.core.remote.ProjectMapper.toProject;
import static io.harness.ng.core.remote.ProjectMapper.writeDTO;
import static io.harness.ng.core.utils.PageUtils.getPageRequest;

import com.google.inject.Inject;

import io.harness.ng.core.RestQueryFilterParser;
import io.harness.ng.core.dto.CreateProjectDTO;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.UpdateProjectDTO;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.services.api.ProjectService;
import io.swagger.annotations.Api;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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

@Api("/projects")
@Path("/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ProjectResource {
  private final ProjectService projectService;
  private final RestQueryFilterParser restQueryFilterParser;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public ProjectDTO create(@NotNull @Valid CreateProjectDTO createProjectDTO) {
    Project project = projectService.create(toProject(createProjectDTO));
    return writeDTO(project);
  }

  @GET
  @Path("{projectId}")
  public Optional<ProjectDTO> get(@PathParam("projectId") @NotEmpty String projectId) {
    Optional<Project> project = projectService.get(projectId);
    return project.map(ProjectMapper::writeDTO);
  }

  @GET
  public Page<ProjectDTO> list(@QueryParam("orgId") String organizationId, @QueryParam("filter") String filterQuery,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("100") int size,
      @QueryParam("sort") List<String> sort) {
    Criteria criteria = restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, Project.class);
    Page<Project> projects;
    if (StringUtils.isNotBlank(organizationId)) {
      projects = projectService.list(organizationId, criteria, getPageRequest(page, size, sort));
    } else {
      projects = projectService.list(criteria, getPageRequest(page, size, sort));
    }
    return projects.map(ProjectMapper::writeDTO);
  }

  @PUT
  @Path("{projectId}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Optional<ProjectDTO> update(
      @PathParam("projectId") @NotEmpty String projectId, @NotNull @Valid UpdateProjectDTO updateProjectDTO) {
    Optional<Project> project = projectService.get(projectId);
    if (project.isPresent()) {
      Project updatedProject = projectService.update(applyUpdateToProject(project.get(), updateProjectDTO));
      return Optional.ofNullable(writeDTO(updatedProject));
    }
    return Optional.empty();
  }

  @DELETE
  @Path("{projectId}")
  @Consumes(MediaType.TEXT_HTML)
  public boolean delete(@PathParam("projectId") @NotEmpty String projectId) {
    return projectService.delete(projectId);
  }
}