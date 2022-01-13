/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.beans.artifact.ArtifactStreamType.ACR;

import static java.lang.String.format;

import io.harness.annotations.dev.BreakDependencyOn;
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

@OwnedBy(CDC)
@BreakDependencyOn("io.harness.ff.FeatureFlagService")
@TargetModule(HarnessModule._957_CG_BEANS)
@JsonTypeName("ACR")
@Data
@EqualsAndHashCode(callSuper = true)
public class AcrArtifactStream extends ArtifactStream {
  @NotEmpty private String subscriptionId;
  @NotEmpty private String registryName;
  private String registryHostName;
  @NotEmpty private String repositoryName;

  public AcrArtifactStream() {
    super(ACR.name());
    super.setMetadataOnly(true);
  }

  @Builder
  public AcrArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, String subscriptionId, String registryName,
      String registryHostName, String repositoryName, String accountId, Set<String> keywords, boolean sample) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, ACR.name(), sourceName,
        settingId, name, autoPopulate, serviceId, true, accountId, keywords, sample);
    this.subscriptionId = subscriptionId;
    this.registryName = registryName;
    this.registryHostName = registryHostName;
    this.repositoryName = repositoryName;
  }

  @Override
  public String fetchArtifactDisplayName(String buildNo) {
    return format("%s_%s_%s", getRegistryName() + "/" + getRepositoryName(), buildNo,
        new SimpleDateFormat(dateFormat).format(new Date()));
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
        .subscriptionId(subscriptionId)
        .registryHostName(registryHostName)
        .registryName(registryName)
        .repositoryName(repositoryName)
        .build();
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes(FeatureFlagService featureFlagService) {
    return ArtifactStreamAttributes.builder()
        .artifactStreamType(getArtifactStreamType())
        .subscriptionId(subscriptionId)
        .registryName(registryName)
        .registryHostName(registryHostName)
        .repositoryName(repositoryName)
        .build();
  }

  @Override
  public boolean shouldValidate() {
    return true;
  }

  @Override
  public String generateSourceName() {
    return getRegistryName() + '/' + getRepositoryName();
  }

  @Override
  public String fetchRepositoryName() {
    return getRepositoryName();
  }

  @Override
  public String fetchRegistryUrl() {
    return getRegistryName();
  }

  @Override
  public boolean checkIfStreamParameterized() {
    return validateParameters(subscriptionId, registryHostName, registryName, repositoryName);
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ArtifactStream.Yaml {
    private String subscriptionId;
    private String registryName;
    private String registryHostName;
    private String repositoryName;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String subscriptionId,
        String registryName, String registryHostName, String repositoryName) {
      super(ACR.name(), harnessApiVersion, serverName);
      this.subscriptionId = subscriptionId;
      this.registryName = registryName;
      this.registryHostName = registryHostName;
      this.repositoryName = repositoryName;
    }
  }
}
