package io.harness.app.resources;

import static io.harness.utils.PageUtils.getPageRequest;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.app.RestQueryFilterParser;
import io.harness.app.beans.dto.CIBuildResponseDTO;
import io.harness.app.intfc.CIBuildInfoService;
import io.harness.ci.beans.entities.CIBuild;
import io.harness.ng.core.dto.ResponseDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("ci")
@Path("/ci/builds")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class CIBuildResource {
  private CIBuildInfoService ciBuildInfoService;
  private final RestQueryFilterParser restQueryFilterParser;

  @GET
  @Path("/{buildIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets a build by identifier", nickname = "getBuild")
  public ResponseDTO<CIBuildResponseDTO> getBuildByIdentifier(
      @NotNull @QueryParam("accountIdentifier") String accountId, @QueryParam("orgIdentifier") String orgId,
      @QueryParam("projectIdentifier") String projectId, @PathParam("buildIdentifier") Long buildId) {
    return ResponseDTO.newResponse(ciBuildInfoService.getBuild(buildId, accountId, orgId, projectId));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Get builds list", nickname = "getBuilds")
  public ResponseDTO<Page<CIBuildResponseDTO>> getBuilds(@NotNull @QueryParam("accountIdentifier") String accountId,
      @QueryParam("orgIdentifier") String orgId, @NotNull @QueryParam("projectIdentifier") String projectId,
      @QueryParam("filter") String filterQuery, @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("10") int size, @QueryParam("sort") List<String> sort) {
    Criteria criteria = restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, CIBuild.class);
    Page<CIBuildResponseDTO> builds =
        ciBuildInfoService.getBuilds(accountId, orgId, projectId, criteria, getPageRequest(page, size, sort));
    return ResponseDTO.newResponse(builds);
  }
}
