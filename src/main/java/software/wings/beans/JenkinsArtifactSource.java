package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import software.wings.app.WingsBootstrap;
import software.wings.service.intfc.FileService;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

public class JenkinsArtifactSource extends ArtifactSource {
  private String jenkinsURL;
  private String username;
  private String password;
  private String jobname;
  private String artifactPathRegex;

  public JenkinsArtifactSource() {
    super(SourceType.JENKINS);
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

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), jenkinsURL, username, password, jobname, artifactPathRegex);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    JenkinsArtifactSource that = (JenkinsArtifactSource) o;
    return Objects.equals(jenkinsURL, that.jenkinsURL) && Objects.equals(username, that.username)
        && Objects.equals(password, that.password) && Objects.equals(jobname, that.jobname)
        && Objects.equals(artifactPathRegex, that.artifactPathRegex);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("jenkinsURL", jenkinsURL)
        .add("username", username)
        .add("password", password)
        .add("jobname", jobname)
        .add("artifactPathRegex", artifactPathRegex)
        .toString();
  }

  public static class Builder {
    private ArtifactType artifactType;
    private String sourceName;
    private String artifactPathRegex;
    private String jobname;
    private String password;
    private String username;
    private String jenkinsURL;

    private Builder() {}

    public Builder but() {
      return aJenkinsArtifactSource()
          .withArtifactType(artifactType)
          .withSourceName(sourceName)
          .withArtifactPathRegex(artifactPathRegex)
          .withJobname(jobname)
          .withPassword(password)
          .withUsername(username)
          .withJenkinsURL(jenkinsURL);
    }

    public Builder withJenkinsURL(String jenkinsURL) {
      this.jenkinsURL = jenkinsURL;
      return this;
    }

    public Builder withUsername(String username) {
      this.username = username;
      return this;
    }

    public Builder withPassword(String password) {
      this.password = password;
      return this;
    }

    public Builder withJobname(String jobname) {
      this.jobname = jobname;
      return this;
    }

    public Builder withArtifactPathRegex(String artifactPathRegex) {
      this.artifactPathRegex = artifactPathRegex;
      return this;
    }

    public Builder withSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    public Builder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    public static Builder aJenkinsArtifactSource() {
      return new Builder();
    }

    public JenkinsArtifactSource build() {
      JenkinsArtifactSource jenkinsArtifactSource = new JenkinsArtifactSource();
      jenkinsArtifactSource.setArtifactType(artifactType);
      jenkinsArtifactSource.setSourceName(sourceName);
      jenkinsArtifactSource.setArtifactPathRegex(artifactPathRegex);
      jenkinsArtifactSource.setJobname(jobname);
      jenkinsArtifactSource.setPassword(password);
      jenkinsArtifactSource.setUsername(username);
      jenkinsArtifactSource.setJenkinsURL(jenkinsURL);
      return jenkinsArtifactSource;
    }
  }
}
