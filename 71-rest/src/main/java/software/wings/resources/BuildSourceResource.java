package software.wings.resources;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.RestResponse;
import software.wings.beans.artifact.Artifact;
import software.wings.common.BuildDetailsComparator;
import software.wings.helpers.ext.gcs.GcsService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.BuildSourceService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
// ToBeRevisited, this resource would be used from both artifact stream and verification step ui.
// Need to find out which auth rule to apply since its only determined at runtime
@Scope(APPLICATION)
public class BuildSourceResource {
  @Inject private BuildSourceService buildSourceService;
  @Inject private GcsService gcsService;

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
   * Get GCS projects
   *
   * @param settingId the setting id
   * @return the project for the service account
   */
  @GET
  @Path("project")
  @Timed
  @ExceptionMetered
  public RestResponse<String> getProject(@QueryParam("appId") String appId, @QueryParam("settingId") String settingId) {
    return new RestResponse<>(buildSourceService.getProject(appId, settingId));
  }

  /**
   * Get GCS buckets
   *
   * @param appId  the app Id
   * @param projectId GCS project Id
   * @param settingId the setting id
   * @return list of buckets
   */
  @GET
  @Path("buckets")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> getBuckets(@QueryParam("appId") String appId,
      @QueryParam("projectId") @NotEmpty String projectId, @QueryParam("settingId") String settingId) {
    return new RestResponse<>(buildSourceService.getBuckets(appId, projectId, settingId));
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
   * Get SMB artifact paths.
   *
   * @param settingId the setting id
   * @return the artifact paths
   */
  @GET
  @Path("smb-paths")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getArtifactPaths(
      @QueryParam("appId") String appId, @QueryParam("settingId") String settingId) {
    return new RestResponse<>(buildSourceService.getSmbPaths(appId, settingId));
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
    List<BuildDetails> buildDetails = buildSourceService.getBuilds(appId, artifactStreamId, settingId);
    buildDetails = buildDetails.stream().sorted(new BuildDetailsComparator()).collect(toList());
    return new RestResponse<>(buildDetails);
  }

  @GET
  @Path("jobs/{jobName}/details")
  @Timed
  @ExceptionMetered
  public RestResponse<JobDetails> getJob(@QueryParam("appId") String appId, @QueryParam("settingId") String settingId,
      @PathParam("jobName") String jobName) {
    return new RestResponse<>(buildSourceService.getJob(appId, settingId, jobName));
  }

  /***
   * Collects an artifact
   * @param appId
   * @param artifactStreamId
   * @param buildDetails
   * @return
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Artifact> collectArtifact(@QueryParam("appId") String appId,
      @QueryParam("artifactStreamId") String artifactStreamId, BuildDetails buildDetails) {
    return new RestResponse<>(buildSourceService.collectArtifact(appId, artifactStreamId, buildDetails));
  }
}
