/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.FeatureName.ENHANCED_GCR_CONNECTIVITY_CHECK;

import static software.wings.beans.artifact.ArtifactStreamType.GCR;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.ff.FeatureFlagService;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author rktummala on 8/4/17.
 */
@OwnedBy(CDC)
@JsonTypeName("GCR")
@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._957_CG_BEANS)
public class GcrArtifactStream extends ArtifactStream {
  @NotEmpty private String registryHostName;
  @NotEmpty private String dockerImageName;

  /**
   * Instantiates a new Docker artifact stream.
   */
  public GcrArtifactStream() {
    super(GCR.name());
    super.setMetadataOnly(true);
  }

  @Builder
  public GcrArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, String registryHostName, String dockerImageName,
      String accountId, Set<String> keywords, boolean sample) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, GCR.name(), sourceName,
        settingId, name, autoPopulate, serviceId, true, accountId, keywords, sample);
    this.registryHostName = registryHostName;
    this.dockerImageName = dockerImageName;
  }

  @Override
  public String fetchArtifactDisplayName(String buildNo) {
    return format("%s_%s_%s", getDockerImageName(), buildNo, new SimpleDateFormat(dateFormat).format(new Date()));
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
        .registryHostName(registryHostName)
        .dockerImageName(dockerImageName)
        .build();
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes(FeatureFlagService featureFlagService) {
    return ArtifactStreamAttributes.builder()
        .artifactStreamType(getArtifactStreamType())
        .imageName(dockerImageName)
        .dockerBasedDeployment(true)
        .registryHostName(registryHostName)
        .enhancedGcrConnectivityCheckEnabled(
            featureFlagService.isEnabled(ENHANCED_GCR_CONNECTIVITY_CHECK, getAccountId()))
        .build();
  }

  @Override
  public String generateSourceName() {
    return getRegistryHostName() + '/' + getDockerImageName();
  }

  @Override
  public String fetchRepositoryName() {
    return dockerImageName;
  }

  @Override
  public String fetchRegistryUrl() {
    return registryHostName;
  }

  @Override
  public boolean shouldValidate() {
    return true;
  }

  @Override
  public boolean checkIfStreamParameterized() {
    return validateParameters(registryHostName, dockerImageName);
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ArtifactStream.Yaml {
    private String registryHostName;
    private String dockerImageName;

    @Builder
    public Yaml(String harnessApiVersion, String serverName, String registryHostName, String dockerImageName) {
      super(GCR.name(), harnessApiVersion, serverName);
      this.registryHostName = registryHostName;
      this.dockerImageName = dockerImageName;
    }
  }
}
