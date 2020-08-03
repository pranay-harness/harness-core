package io.harness.ng.core.remote;

import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;

import io.harness.beans.NGPageResponse;
import io.harness.ng.core.RestQueryFilterParser;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
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
import org.springframework.data.mongodb.core.query.TextCriteria;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("account-projects")
@Path("projects")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class AccountProjectResource {
  private final ProjectService projectService;
  private final OrganizationService organizationService;
  private final RestQueryFilterParser restQueryFilterParser;

  @GET
  @ApiOperation(value = "Gets Project list based on filter", nickname = "getProjectListBasedOnFilter")
  public ResponseDTO<NGPageResponse<ProjectDTO>> listProjectsBasedOnFilter(
      @QueryParam("accountIdentifier") String accountIdentifier,
      @QueryParam("filterQuery") @DefaultValue("") String filterQuery, @QueryParam("search") String search,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("100") int size,
      @QueryParam("sort") List<String> sort) {
    Criteria criteria = restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, Project.class)
                            .and(ProjectKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(ProjectKeys.deleted)
                            .ne(Boolean.TRUE);
    Page<ProjectDTO> projects;
    if (isNotBlank(search)) {
      TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matching(search);
      projects =
          projectService.list(textCriteria, criteria, getPageRequest(page, size, sort)).map(ProjectMapper::writeDTO);
    } else {
      projects = projectService.list(criteria, getPageRequest(page, size, sort)).map(ProjectMapper::writeDTO);
    }
    List<String> orgIdentifiers =
        projects.getContent().stream().map(ProjectDTO::getOrgIdentifier).collect(Collectors.toList());
    Criteria orgCriteria = Criteria.where(OrganizationKeys.accountIdentifier)
                               .is(accountIdentifier)
                               .and(OrganizationKeys.identifier)
                               .in(orgIdentifiers);
    Map<String, String> map = organizationService.list(orgCriteria, getPageRequest(page, size, sort))
                                  .getContent()
                                  .stream()
                                  .collect(Collectors.toMap(Organization::getIdentifier, Organization::getName));
    projects.getContent().forEach(projectDTO -> projectDTO.setOrganizationName(map.get(projectDTO.getOrgIdentifier())));
    return ResponseDTO.newResponse(getNGPageResponse(projects));
  }
}
