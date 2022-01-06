/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;

import software.wings.utils.RepositoryType;
import software.wings.utils.Utils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
@JsonTypeName("ARTIFACTORY")
@Data
@EqualsAndHashCode(callSuper = true)
@BreakDependencyOn("io.harness.ff.FeatureFlagService")
public class ArtifactoryArtifactStream extends ArtifactStream {
  private String repositoryType = "any";
  @NotEmpty private String jobname;
  private String imageName;
  private List<String> artifactPaths;
  private String artifactPattern;
  private String dockerRepositoryServer;
  private boolean useDockerFormat;

  public ArtifactoryArtifactStream() {
    super(ARTIFACTORY.name());
  }

  @Builder
  public ArtifactoryArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, boolean metadataOnly, String repositoryType, String jobname,
      String imageName, List<String> artifactPaths, String artifactPattern, String dockerRepositoryServer,
      boolean useDockerFormat, String accountId, Set<String> keywords, boolean sample) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, ARTIFACTORY.name(),
        sourceName, settingId, name, autoPopulate, serviceId, metadataOnly, accountId, keywords, sample);
    this.repositoryType = repositoryType;
    this.jobname = jobname;
    this.imageName = imageName;
    this.artifactPaths = artifactPaths;
    this.artifactPattern = artifactPattern;
    this.dockerRepositoryServer = dockerRepositoryServer;
    this.useDockerFormat = useDockerFormat;
  }

  @Override
  public String fetchArtifactDisplayName(String buildNo) {
    return isBlank(getImageName())
        ? format("%s_%s_%s", getSourceName(), buildNo, new SimpleDateFormat(dateFormat).format(new Date()))
        : format("%s_%s_%s", getJobname() + "/" + getImageName(), buildNo,
            new SimpleDateFormat(dateFormat).format(new Date()));
  }

  @Override
  public String fetchRepositoryName() {
    return imageName;
  }

  @Override
  public String generateName() {
    return Utils.normalize(generateSourceName());
  }

  @Override
  public String generateSourceName() {
    StringBuilder builder = new StringBuilder(getJobname());
    if (isNotEmpty(artifactPattern)) {
      builder.append('/').append(getArtifactPattern());
    } else {
      builder.append('/').append(getImageName());
    }
    return builder.toString();
  }

  // TODO: remove this method after migration of old artifact streams
  // TODO: add validations for repository type
  public String getRepositoryType() {
    if (isEmpty(artifactPattern)) {
      return RepositoryType.docker.name();
    }
    return repositoryType;
  }

  @Override
  public boolean artifactSourceChanged(ArtifactStream artifactStream) {
    if (super.artifactSourceChanged(artifactStream)) {
      return true;
    }

    // NOTE: UI is not passing repository type for docker artifact type. So we have to check for the default value "any"
    // also here.
    if (this.repositoryType.equals(RepositoryType.docker.name())
        || this.repositoryType.equals(RepositoryType.any.name())) {
      return repositoryServerChanged(((ArtifactoryArtifactStream) artifactStream).dockerRepositoryServer);
    }
    return false;
  }

  private boolean repositoryServerChanged(String dockerRepositoryServer) {
    if (isEmpty(this.dockerRepositoryServer) || isEmpty(dockerRepositoryServer)) {
      return isNotEmpty(this.dockerRepositoryServer) || isNotEmpty(dockerRepositoryServer);
    }
    return !this.dockerRepositoryServer.equals(dockerRepositoryServer);
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes(FeatureFlagService featureFlagService) {
    return ArtifactStreamAttributes.builder()
        .artifactStreamType(getArtifactStreamType())
        .jobName(jobname)
        .imageName(imageName)
        .artifactPattern(artifactPattern)
        .artifactName(artifactPaths == null ? "" : artifactPaths.get(0))
        .repositoryType(getRepositoryType())
        .metadataOnly(isMetadataOnly())
        .artifactoryDockerRepositoryServer(dockerRepositoryServer)
        .dockerBasedDeployment(useDockerFormat)
        .build();
  }

  @Override
  public void validateRequiredFields() {
    if (appId.equals(GLOBAL_APP_ID)) {
      if (isEmpty(repositoryType)) {
        throw new InvalidRequestException("Repository Type cannot be empty", USER);
      }
    }
  }

  @Override
  public ArtifactStream cloneInternal() {
    return builder()
        .appId(getAppId())
        .accountId(getAccountId())
        .name(getName())
        .sourceName(getSourceName())
        .settingId(getSettingId())
        .keywords(getKeywords())
        .repositoryType(repositoryType)
        .jobname(jobname)
        .imageName(imageName)
        .artifactPaths(artifactPaths)
        .artifactPattern(artifactPattern)
        .dockerRepositoryServer(dockerRepositoryServer)
        .useDockerFormat(useDockerFormat)
        .build();
  }

  @Override
  public boolean shouldValidate() {
    return true;
  }

  @Override
  public boolean checkIfStreamParameterized() {
    if (isNotEmpty(artifactPaths)) {
      return validateParameters(jobname, imageName, artifactPaths.get(0), artifactPattern, dockerRepositoryServer);
    }
    return validateParameters(jobname, imageName, artifactPattern, dockerRepositoryServer);
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends ArtifactStream.Yaml {
    private String repositoryName;
    private String groupId;
    private String imageName;
    private List<String> artifactPaths;
    private String artifactPattern;
    private String repositoryType;
    private String dockerRepositoryServer;
    private boolean metadataOnly;
    private boolean useDockerFormat;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String repositoryName,
        String imageName, List<String> artifactPaths, String artifactPattern, String repositoryType,
        boolean useDockerFormat) {
      super(ARTIFACTORY.name(), harnessApiVersion, serverName);
      this.repositoryName = repositoryName;
      this.imageName = imageName;
      this.artifactPaths = artifactPaths;
      this.artifactPattern = artifactPattern;
      this.repositoryType = repositoryType;
      this.metadataOnly = metadataOnly;
      this.useDockerFormat = useDockerFormat;
    }
  }
}
