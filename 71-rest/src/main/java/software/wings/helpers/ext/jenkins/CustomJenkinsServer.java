package software.wings.helpers.ext.jenkins;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.client.JenkinsHttpClient;
import com.offbytwo.jenkins.client.util.EncodingUtils;
import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.JobWithDetails;
import io.harness.annotations.dev.OwnedBy;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import software.wings.helpers.ext.jenkins.model.JobWithExtendedDetails;

import java.io.IOException;

@OwnedBy(CDC)
public class CustomJenkinsServer extends JenkinsServer {
  private JenkinsHttpClient client;

  public CustomJenkinsServer(JenkinsHttpClient client) {
    super(client);
    this.client = client;
  }

  @Override
  public JobWithDetails getJob(FolderJob folder, String jobName) throws IOException {
    try {
      JobWithExtendedDetails job = client.get(toJobUrl(folder, jobName), JobWithExtendedDetails.class);
      job.setClient(client);

      return job;
    } catch (HttpResponseException e) {
      if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
        return null;
      }
      throw e;
    }
  }

  /**
   * Helper to create the base url for a job, with or without a given folder
   *
   * @param folder the folder or {@code null}
   * @param jobName the name of the job.
   * @return converted base url.
   */
  private String toJobUrl(FolderJob folder, String jobName) {
    return toBaseJobUrl(folder) + "job/" + EncodingUtils.encode(jobName);
  }

  /**
   * Helper to create a base url in case a folder is given
   *
   * @param folder the folder or {@code null}
   * @return The created base url.
   */
  private String toBaseJobUrl(FolderJob folder) {
    String path = "/";
    if (folder != null) {
      path = folder.getUrl();
    }
    return path;
  }
}
