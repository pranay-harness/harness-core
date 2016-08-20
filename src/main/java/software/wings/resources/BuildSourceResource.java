package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.BuildSourceService;

import java.util.List;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 8/18/16.
 */

@Api("build-sources")
@Path("/build-sources")
@Timed
@ExceptionMetered
@Produces("application/json")
public class BuildSourceResource {
  @Inject private BuildSourceService buildSourceService;

  @GET
  @Path("jobs")
  public RestResponse<Set<String>> getJobs(@QueryParam("settingId") String settingId) {
    return new RestResponse<>(buildSourceService.getJobs(settingId));
  }

  @GET
  @Path("jobs/{jobName}/paths")
  public RestResponse<Set<String>> getArtifactPaths(
      @PathParam("jobName") String jobName, @QueryParam("settingId") String settingId) {
    return new RestResponse<>(buildSourceService.getArtifactPaths(jobName, settingId));
  }

  @GET
  @Path("builds")
  public RestResponse<List<BuildDetails>> getBuilds(@QueryParam("appId") String appId,
      @QueryParam("releaseId") String releaseId, @QueryParam("artifactSourceName") String artifactSourceName,
      @QueryParam("settingId") String settingId) {
    return new RestResponse<>(buildSourceService.getBuilds(appId, releaseId, artifactSourceName, settingId));
  }
}
