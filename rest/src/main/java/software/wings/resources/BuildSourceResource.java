package software.wings.resources;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
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
@Produces("application/json")
@AuthRule(ResourceType.APPLICATION)
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
  @Timed
  @ExceptionMetered
  public RestResponse<Set<JobDetails>> getJobs(@QueryParam("appId") String appId,
      @QueryParam("settingId") String settingId, @QueryParam("parentJobName") String parentJobName) {
    return new RestResponse<>(buildSourceService.getJobs(appId, settingId, parentJobName));
  }

  /**
   * Gets bamboo plans.
   *
   * @param settingId the setting id
   * @return the bamboo plans
   */
  @GET
  @Path("plans")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> getBuildPlans(@QueryParam("appId") String appId,
      @QueryParam("settingId") String settingId, @QueryParam("serviceId") String serviceId,
      @QueryParam("streamType") String streamType, @QueryParam("repositoryType") String repositoryType) {
    if (isBlank(serviceId)) {
      return new RestResponse<>(buildSourceService.getPlans(appId, settingId, streamType));
    }
    return new RestResponse<>(buildSourceService.getPlans(appId, settingId, serviceId, streamType, repositoryType));
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
  @Timed
  @ExceptionMetered
  public RestResponse<Set<String>> getArtifactPaths(@QueryParam("appId") String appId,
      @PathParam("jobName") String jobName, @QueryParam("settingId") String settingId,
      @QueryParam("groupId") String groupId, @QueryParam("streamType") String streamType) {
    return new RestResponse<>(buildSourceService.getArtifactPaths(appId, jobName, settingId, groupId, streamType));
  }

  /**
   * Gets artifact paths.
   *
   * @param jobName   the job name
   * @param settingId the setting id
   * @return group Ids
   */
  @GET
  @Path("jobs/{jobName}/groupIds")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<String>> getGroupIds(@QueryParam("appId") String appId, @PathParam("jobName") String jobName,
      @QueryParam("settingId") String settingId) {
    return new RestResponse<>(buildSourceService.getGroupIds(appId, jobName, settingId));
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
  @Timed
  @ExceptionMetered
  public RestResponse<List<BuildDetails>> getBuilds(@QueryParam("appId") String appId,
      @QueryParam("artifactStreamId") String artifactStreamId, @QueryParam("settingId") String settingId) {
    return new RestResponse<>(buildSourceService.getBuilds(appId, artifactStreamId, settingId));
  }

  @GET
  @Path("jobs/{jobName}/details")
  @Timed
  @ExceptionMetered
  public RestResponse<JobDetails> getJob(@QueryParam("appId") String appId, @QueryParam("settingId") String settingId,
      @PathParam("jobName") String jobName) {
    return new RestResponse<>(buildSourceService.getJob(appId, settingId, jobName));
  }
}
