package software.wings.helpers.ext;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class Jenkins {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private String jenkinsUrl;
  private String username;
  private String password;
  private JenkinsServer jenkinsServer;

  public Jenkins(String jenkinsUrl) throws URISyntaxException {
    this.jenkinsUrl = jenkinsUrl;
    jenkinsServer = new JenkinsServer(new URI(jenkinsUrl));
  }

  public Jenkins(String jenkinsUrl, String username, String password) throws URISyntaxException {
    this.jenkinsUrl = jenkinsUrl;
    this.username = username;
    this.password = password;
    jenkinsServer = new JenkinsServer(new URI(jenkinsUrl), username, password);
  }

  public boolean getJob(String jobname) throws IOException {
    JobWithDetails jobDetails = jenkinsServer.getJob(jobname);
    if (jobDetails != null) {
      return true;
    }
    return false;
  }

  public String trigger(String jobname) {
    return jobname;
  }

  public String checkStatus(String jobname) {
    return jobname;
  }

  public String checkStatus(String jobname, String buildNo) {
    return jobname;
  }

  public String checkArtifactStatus(String jobname, String artifactpathRegex) {
    return jobname;
  }

  public String checkArtifactStatus(String jobname, String buildNo, String artifactpathRegex) {
    return jobname;
  }

  public void downloadArtifact(String jobname, String artifactpathRegex, OutputStream os) throws IOException {
    /*Pattern pattern = Pattern.compile(artifactpathRegex);
    JobWithDetails jobDetails = jenkinsServer.getJob(jobname);
    BuildWithDetails buildWithDetails = jobDetails.getLastCompletedBuild().details();
    Optional<Artifact> artifactOpt = buildWithDetails
        .getArtifacts().stream().filter(artifact -> pattern.matcher(artifact.getRelativePath()).matches()).findFirst();
    if(artifactOpt.isPresent()) {

    }*/
  }

  public void downloadArtifact(String jobname, String buildNo, String artifactpathRegex, OutputStream os) {}
}
