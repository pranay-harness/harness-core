package software.wings.helpers.ext;

import java.io.OutputStream;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.JobWithDetails;

public class Jenkins {
  private String jenkinsURL;
  private String username;
  private String password;

  public Jenkins(String jenkinsURL) {
    this.jenkinsURL = jenkinsURL;
  }

  public Jenkins(String jenkinsURL, String username, String password) {
    this.jenkinsURL = jenkinsURL;
    this.username = username;
    this.password = password;
  }

  public boolean jobExists(String jobname) {
    try {
      JenkinsServer jenkins = new JenkinsServer(new URI(jenkinsURL), username, password);
      JobWithDetails jobDetails = jenkins.getJob(jobname);
      if (jobDetails != null) {
        return true;
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
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

  public void downloadArtifact(String jobname, String artifactpathRegex, OutputStream os) {}

  public void downloadArtifact(String jobname, String buildNo, String artifactpathRegex, OutputStream os) {}

  private static Logger logger = LoggerFactory.getLogger(Jenkins.class);
}
