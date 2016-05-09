package software.wings.beans;

import static java.util.stream.Collectors.toSet;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.service.intfc.FileService;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;

public class JenkinsArtifactSource extends ArtifactSource {
  @Inject private FileService fileService;

  @NotEmpty private String jenkinsUrl;

  @NotEmpty private String username;

  @NotEmpty private String password;

  @NotEmpty private String jobname;

  @NotEmpty @Valid private List<ArtifactPathServiceEntry> artifactPathServices = Lists.newArrayList();

  public JenkinsArtifactSource() {
    super(SourceType.JENKINS);
  }

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

  @Override
  public Set<String> getServiceIds() {
    return artifactPathServices.stream()
        .flatMap(artifactPathServiceEntry -> artifactPathServiceEntry.getServiceIds().stream())
        .collect(toSet());
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

  public List<ArtifactPathServiceEntry> getArtifactPathServices() {
    return artifactPathServices;
  }

  public void setArtifactPathServices(List<ArtifactPathServiceEntry> artifactPathServices) {
    this.artifactPathServices = artifactPathServices;
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
    JenkinsArtifactSource that = (JenkinsArtifactSource) obj;
    return Objects.equal(jenkinsUrl, that.jenkinsUrl) && Objects.equal(username, that.username)
        && Objects.equal(password, that.password) && Objects.equal(jobname, that.jobname)
        && Objects.equal(artifactPathServices, that.artifactPathServices);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), jenkinsUrl, username, password, jobname, artifactPathServices);
  }

  @Override
  public String getSourceName() {
    if (super.getSourceName() == null) {
      setSourceName(jobname);
    }
    return super.getSourceName();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("jenkinsUrl", jenkinsUrl)
        .add("username", username)
        .add("password", password)
        .add("jobname", jobname)
        .add("artifactPathServices", artifactPathServices)
        .add("sourceName", getSourceName())
        .toString();
  }

  public static final class Builder {
    private String sourceName;
    private ArtifactType artifactType;
    private String jobname;
    private List<ArtifactPathServiceEntry> artifactPathServices = Lists.newArrayList();
    private String username;
    private String password;
    private String jenkinsUrl;

    private Builder() {}

    public static Builder aJenkinsArtifactSource() {
      return new Builder();
    }

    public Builder withSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    public Builder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    public Builder withJobname(String jobname) {
      this.jobname = jobname;
      return this;
    }

    public Builder withArtifactPathServices(List<ArtifactPathServiceEntry> artifactPathServices) {
      this.artifactPathServices = artifactPathServices;
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

    public Builder withJenkinsUrl(String jenkinsUrl) {
      this.jenkinsUrl = jenkinsUrl;
      return this;
    }

    /**
     * @return a new JenkinsArtifactSource object with given fields.
     */
    public JenkinsArtifactSource build() {
      JenkinsArtifactSource jenkinsArtifactSource = new JenkinsArtifactSource();
      jenkinsArtifactSource.setSourceName(sourceName);
      jenkinsArtifactSource.setArtifactType(artifactType);
      jenkinsArtifactSource.setJobname(jobname);
      jenkinsArtifactSource.setArtifactPathServices(artifactPathServices);
      jenkinsArtifactSource.setUsername(username);
      jenkinsArtifactSource.setPassword(password);
      jenkinsArtifactSource.setJenkinsUrl(jenkinsUrl);
      return jenkinsArtifactSource;
    }
  }
}
