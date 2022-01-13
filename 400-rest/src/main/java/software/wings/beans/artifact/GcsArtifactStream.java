/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.artifact.ArtifactStreamType.GCS;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.ff.FeatureFlagService;

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
@JsonTypeName("GCS")
@Data
@EqualsAndHashCode(callSuper = true)
public class GcsArtifactStream extends ArtifactStream {
  @NotEmpty private String jobname;
  @NotEmpty private List<String> artifactPaths;
  @NotEmpty private String projectId;

  public GcsArtifactStream() {
    super(GCS.name());
    super.setMetadataOnly(true);
  }

  @Builder
  public GcsArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, String jobname, List<String> artifactPaths, String projectId,
      String accountId, Set<String> keywords, boolean sample) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, GCS.name(), sourceName,
        settingId, name, autoPopulate, serviceId, true, accountId, keywords, sample);
    this.jobname = jobname;
    this.artifactPaths = artifactPaths;
    this.projectId = projectId;
  }

  @Override
  public String fetchArtifactDisplayName(String buildNo) {
    return isBlank(getSourceName())
        ? format("%s_%s_%s", getSourceName(), buildNo, new SimpleDateFormat(dateFormat).format(new Date()))
        : format("%s_%s_%s", getJobname(), buildNo, new SimpleDateFormat(dateFormat).format(new Date()));
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
        .jobname(jobname)
        .artifactPaths(artifactPaths)
        .projectId(projectId)
        .build();
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes(FeatureFlagService featureFlagService) {
    return ArtifactStreamAttributes.builder()
        .artifactStreamType(getArtifactStreamType())
        .jobName(jobname)
        .artifactName(artifactPaths == null ? "" : artifactPaths.get(0))
        .build();
  }

  @Override
  public String generateSourceName() {
    return getArtifactPaths().stream().map(artifactPath -> '/' + artifactPath).collect(joining("", getJobname(), ""));
  }

  @Override
  public boolean checkIfStreamParameterized() {
    if (isNotEmpty(artifactPaths)) {
      return validateParameters(jobname, artifactPaths.get(0), projectId);
    }
    return validateParameters(jobname, projectId);
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends ArtifactStream.Yaml {
    private String bucketName;
    private List<String> artifactPaths;
    private String projectId;

    @lombok.Builder
    public Yaml(
        String harnessApiVersion, String serverName, String bucketName, List<String> artifactPaths, String projectId) {
      super(GCS.name(), harnessApiVersion, serverName);
      this.bucketName = bucketName;
      this.artifactPaths = artifactPaths;
      this.projectId = projectId;
    }
  }
}
