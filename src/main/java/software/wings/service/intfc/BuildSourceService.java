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
   * @param appId              the app id
   * @param releaseId          the release id
   * @param artifactSourceName the artifact source name
   * @param settingId          the jenkins setting id
   * @return the builds
   */
  List<BuildDetails> getBuilds(@NotEmpty String appId, @NotEmpty String releaseId, @NotEmpty String artifactSourceName,
      @NotEmpty String settingId);
}
