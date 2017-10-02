package software.wings.helpers.ext.jenkins;

import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.offbytwo.jenkins.model.QueueReference;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Created by peeyushaggarwal on 5/12/16.
 */
public interface Jenkins {
  /**
   * Gets the job.
   *
   * @param jobname the jobname
   * @return the job
   * @throws IOException Signals that an I/O exception has occurred.
   */
  JobWithDetails getJob(String jobname) throws IOException;

  /**
   * Gets the child jobs for the given parent folder job. For the root level jobs, pass null.
   * @param parentFolderJobName parent folder job name. To get the root level jobs, pass null.
   * @return
   * @throws IOException
   */
  List<JobDetails> getJobs(@Nullable String parentFolderJobName) throws IOException;

  /**
   * Gets the builds for job.
   *
   * @param jobname the jobname
   * @param lastN   the last n
   * @return the builds for job
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<BuildDetails> getBuildsForJob(String jobname, int lastN) throws IOException;

  /**
   * Gets last successful build for job.
   *
   * @param jobName the job name
   * @return the last successful build for job
   * @throws IOException the io exception
   */
  BuildDetails getLastSuccessfulBuildForJob(String jobName) throws IOException;

  /**
   * Trigger queue reference.
   *
   * @param jobname    the jobname
   * @param parameters the parameters
   * @return the queue reference
   * @throws IOException the io exception
   */
  QueueReference trigger(String jobname, Map<String, String> parameters) throws IOException;

  /**
   * Check status.
   *
   * @param jobname the jobname
   * @return the string
   */
  String checkStatus(String jobname);

  /**
   * Check status.
   *
   * @param jobname the jobname
   * @param buildNo the build no
   * @return the string
   */
  String checkStatus(String jobname, String buildNo);

  /**
   * Check artifact status.
   *
   * @param jobname           the jobname
   * @param artifactpathRegex the artifactpath regex
   * @return the string
   */
  String checkArtifactStatus(String jobname, String artifactpathRegex);

  /**
   * Check artifact status.
   *
   * @param jobname           the jobname
   * @param buildNo           the build no
   * @param artifactpathRegex the artifactpath regex
   * @return the string
   */
  String checkArtifactStatus(String jobname, String buildNo, String artifactpathRegex);

  /**
   * Gets build.
   *
   * @param queueItem the queue item
   * @return the build
   * @throws IOException the io exception
   */
  Build getBuild(QueueReference queueItem) throws IOException;

  boolean isRunning();

  /**
   * downloads the artifacts from the jenkins server
   * @param jobName       the jenkins job name
   * @param buildNo       the build number
   * @param artifactPaths list of artifact paths configured in the artifact source / stream
   * @param delegateId    delegate id
   * @param taskId        task id
   * @param accountId     account id
   * @return list notify response data
   * @throws IOException
   * @throws URISyntaxException
   */
  ListNotifyResponseData downloadArtifacts(String jobName, String buildNo, List<String> artifactPaths,
      String delegateId, String taskId, String accountId) throws IOException, URISyntaxException;
}
