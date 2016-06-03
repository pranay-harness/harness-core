package software.wings.helpers.ext.jenkins;

import com.offbytwo.jenkins.model.JobWithDetails;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

// TODO: Auto-generated Javadoc

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
   * Gets the builds for job.
   *
   * @param jobname the jobname
   * @param lastN   the last n
   * @return the builds for job
   * @throws IOException Signals that an I/O exception has occurred.
   */
  List<BuildDetails> getBuildsForJob(String jobname, int lastN) throws IOException;

  /**
   * Trigger.
   *
   * @param jobname the jobname
   * @return the string
   */
  String trigger(String jobname);

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
   * Download artifact.
   *
   * @param jobname           the jobname
   * @param artifactpathRegex the artifactpath regex
   * @return the pair
   * @throws IOException        Signals that an I/O exception has occurred.
   * @throws URISyntaxException the URI syntax exception
   */
  Pair<String, InputStream> downloadArtifact(String jobname, String artifactpathRegex)
      throws IOException, URISyntaxException;

  /**
   * Download artifact.
   *
   * @param jobname           the jobname
   * @param buildNo           the build no
   * @param artifactpathRegex the artifactpath regex
   * @return the pair
   * @throws IOException        Signals that an I/O exception has occurred.
   * @throws URISyntaxException the URI syntax exception
   */
  Pair<String, InputStream> downloadArtifact(String jobname, String buildNo, String artifactpathRegex)
      throws IOException, URISyntaxException;
}
