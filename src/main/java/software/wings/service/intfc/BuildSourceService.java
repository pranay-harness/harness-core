package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Set;

/**
 * Created by anubhaw on 8/18/16.
 */
public interface BuildSourceService {
  /**
   * Gets jobs.
   *
   * @param settingId the jenkins setting id
   * @return the jobs
   */
  Set<String> getJobs(@NotEmpty String settingId);

  /**
   * Gets artifact paths.
   *
   * @param jobName   the job name
   * @param settingId the setting id
   * @return the artifact paths
   */
  Set<String> getArtifactPaths(@NotEmpty String jobName, @NotEmpty String settingId);

  /**
   * Gets builds.
   *
   * @param artifactStreamId the artifact source id
   * @param appId            the app id
   * @param settingId        the setting id
   * @return the builds
   */
  List<BuildDetails> getBuilds(String artifactStreamId, String appId, String settingId);
}
