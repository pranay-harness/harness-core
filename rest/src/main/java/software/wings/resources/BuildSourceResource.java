package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.BuildSourceService;

import java.util.List;
import java.util.Map;
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

  /**
   * Gets jobs.
   *
   * @param settingId the setting id
   * @return the jobs
   */
  @GET
  @Path("jobs")
  public RestResponse<Set<String>> getJobs(@QueryParam("settingId") String settingId) {
    return new RestResponse<>(buildSourceService.getJobs(settingId));
  }

  /**
   * Gets bamboo plans.
   *
   * @param settingId the setting id
   * @return the bamboo plans
   */
  @GET
  @Path("plans")
  public RestResponse<Map<String, String>> getBambooPlans(@QueryParam("settingId") String settingId) {
    return new RestResponse<>(buildSourceService.getPlans(settingId));
  }

  /**
   * Gets artifact paths.
   *
   * @param jobName   the job name
   * @param settingId the setting id
   * @return the artifact paths
   */
  @GET
  @Path("jobs/{jobName}/paths")
  public RestResponse<Set<String>> getArtifactPaths(
      @PathParam("jobName") String jobName, @QueryParam("settingId") String settingId) {
    return new RestResponse<>(buildSourceService.getArtifactPaths(jobName, settingId));
  }

  /**
   * Gets builds.
   *
   * @param appId            the app id
   * @param artifactStreamId the artifact source id
   * @param settingId        the setting id
   * @return the builds
   */
  @GET
  @Path("builds")
  public RestResponse<List<BuildDetails>> getBuilds(@QueryParam("appId") String appId,
      @QueryParam("artifactStreamId") String artifactStreamId, @QueryParam("settingId") String settingId) {
    return new RestResponse<>(buildSourceService.getBuilds(appId, artifactStreamId, settingId));
  }
}
