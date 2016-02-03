package software.wings.beans;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.commons.io.IOUtils;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;

import software.wings.app.WingsBootstrap;
import software.wings.service.intfc.FileService;

public class JenkinsArtifactSource extends ArtifactSource {
  public JenkinsArtifactSource() {
    super(SourceType.JENKINS);
  }

  private String jenkinsURL;
  private String username;
  private String password;

  private String jobname;
  private String artifactPathRegex;

  @Override
  public ArtifactFile collect(Object[] params) {
    try {
      JenkinsServer jenkins = new JenkinsServer(new URI(jenkinsURL), username, password);
      JobWithDetails jobDetails = jenkins.getJob(jobname);
      Build build = jobDetails.getLastBuild();
      BuildWithDetails buildWithDetails = build.details();
      Artifact buildArtifact = buildWithDetails.getArtifacts().get(0);
      InputStream in = buildWithDetails.downloadArtifact(buildArtifact);

      FileService fileService = WingsBootstrap.lookup(FileService.class);
      FileMetadata fileMetadata = new FileMetadata();
      fileMetadata.setFileName(buildArtifact.getFileName());
      String uuid = fileService.saveFile(fileMetadata, in);
      ArtifactFile artifactFile = new ArtifactFile();
      artifactFile.setFileUUID(uuid);
      artifactFile.setFileName(buildArtifact.getFileName());
      in.close();
      return artifactFile;
    } catch (Exception e) {
      return null;
    }
  }

  public String getJenkinsURL() {
    return jenkinsURL;
  }

  public void setJenkinsURL(String jenkinsURL) {
    this.jenkinsURL = jenkinsURL;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getJobname() {
    return jobname;
  }

  public void setJobname(String jobname) {
    this.jobname = jobname;
  }

  public String getArtifactPathRegex() {
    return artifactPathRegex;
  }

  public void setArtifactPathRegex(String artifactPathRegex) {
    this.artifactPathRegex = artifactPathRegex;
  }

  @Override
  public String getSourceName() {
    if (super.getSourceName() == null) {
      setSourceName(jobname);
    }
    return super.getSourceName();
  }
}
