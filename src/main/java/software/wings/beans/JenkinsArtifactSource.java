package software.wings.beans;

import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import software.wings.service.intfc.FileService;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

public class JenkinsArtifactSource extends ArtifactSource {
  @Inject private FileService fileService;

  public JenkinsArtifactSource() {
    super(SourceType.JENKINS);
  }

  private String jenkinsUrl;
  private String username;
  private String password;

  private String jobname;
  private String artifactPathRegex;

  @Override
  public ArtifactFile collect(Object[] params) {
    try {
      JenkinsServer jenkins = new JenkinsServer(new URI(jenkinsUrl), username, password);
      JobWithDetails jobDetails = jenkins.getJob(jobname);
      Build build = jobDetails.getLastBuild();
      BuildWithDetails buildWithDetails = build.details();
      Artifact buildArtifact = buildWithDetails.getArtifacts().get(0);
      InputStream in = buildWithDetails.downloadArtifact(buildArtifact);

      FileMetadata fileMetadata = new FileMetadata();
      fileMetadata.setFileName(buildArtifact.getFileName());
      String uuid = fileService.saveFile(fileMetadata, in, ARTIFACTS);
      ArtifactFile artifactFile = new ArtifactFile();
      artifactFile.setFileUuid(uuid);
      artifactFile.setFileName(buildArtifact.getFileName());
      in.close();
      return artifactFile;
    } catch (Exception ex) {
      return null;
    }
  }

  public String getJenkinsUrl() {
    return jenkinsUrl;
  }

  public void setJenkinsUrl(String jenkinsUrl) {
    this.jenkinsUrl = jenkinsUrl;
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
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(fileService, jenkinsUrl, username, password, jobname, artifactPathRegex);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final JenkinsArtifactSource other = (JenkinsArtifactSource) obj;
    return Objects.equals(this.fileService, other.fileService) && Objects.equals(this.jenkinsUrl, other.jenkinsUrl)
        && Objects.equals(this.username, other.username) && Objects.equals(this.password, other.password)
        && Objects.equals(this.jobname, other.jobname)
        && Objects.equals(this.artifactPathRegex, other.artifactPathRegex);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("jenkinsUrl", jenkinsUrl)
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
    private String jenkinsUrl;

    private Builder() {}

    public static Builder aJenkinsArtifactSource() {
      return new Builder();
    }

    public Builder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    public Builder withSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    public Builder withArtifactPathRegex(String artifactPathRegex) {
      this.artifactPathRegex = artifactPathRegex;
      return this;
    }

    public Builder withJobname(String jobname) {
      this.jobname = jobname;
      return this;
    }

    public Builder withPassword(String password) {
      this.password = password;
      return this;
    }

    public Builder withUsername(String username) {
      this.username = username;
      return this;
    }

    public Builder withJenkinsUrl(String jenkinsUrl) {
      this.jenkinsUrl = jenkinsUrl;
      return this;
    }

    public Builder but() {
      return aJenkinsArtifactSource()
          .withArtifactType(artifactType)
          .withSourceName(sourceName)
          .withArtifactPathRegex(artifactPathRegex)
          .withJobname(jobname)
          .withPassword(password)
          .withUsername(username)
          .withJenkinsUrl(jenkinsUrl);
    }

    public JenkinsArtifactSource build() {
      JenkinsArtifactSource jenkinsArtifactSource = new JenkinsArtifactSource();
      jenkinsArtifactSource.setArtifactType(artifactType);
      jenkinsArtifactSource.setSourceName(sourceName);
      jenkinsArtifactSource.setArtifactPathRegex(artifactPathRegex);
      jenkinsArtifactSource.setJobname(jobname);
      jenkinsArtifactSource.setPassword(password);
      jenkinsArtifactSource.setUsername(username);
      jenkinsArtifactSource.setJenkinsUrl(jenkinsUrl);
      return jenkinsArtifactSource;
    }
  }
}
