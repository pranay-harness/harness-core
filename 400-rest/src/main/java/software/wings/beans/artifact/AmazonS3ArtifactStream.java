/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.BreakDependencyOn;
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
@BreakDependencyOn("io.harness.ff.FeatureFlagService")
@TargetModule(HarnessModule._957_CG_BEANS)
@JsonTypeName("AMAZON_S3")
@Data
@EqualsAndHashCode(callSuper = true)
public class AmazonS3ArtifactStream extends ArtifactStream {
  @NotEmpty private String jobname;
  @NotEmpty private List<String> artifactPaths;

  public AmazonS3ArtifactStream() {
    super(AMAZON_S3.name());
    super.setMetadataOnly(true);
  }

  @Builder
  public AmazonS3ArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, String jobname, List<String> artifactPaths, String accountId,
      Set<String> keywords, boolean sample) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, AMAZON_S3.name(), sourceName,
        settingId, name, autoPopulate, serviceId, true, accountId, keywords, sample);
    this.jobname = jobname;
    this.artifactPaths = artifactPaths;
  }

  @Override
  public String fetchArtifactDisplayName(String buildNum) {
    return isBlank(getSourceName())
        ? format("%s_%s_%s", getSourceName(), buildNum, new SimpleDateFormat(dateFormat).format(new Date()))
        : format("%s_%s_%s", getJobname(), buildNum, new SimpleDateFormat(dateFormat).format(new Date()));
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
      return validateParameters(jobname, artifactPaths.get(0));
    }
    return validateParameters(jobname);
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends ArtifactStream.Yaml {
    private String bucketName;
    private List<String> artifactPaths;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, String bucketName, List<String> artifactPaths) {
      super(AMAZON_S3.name(), harnessApiVersion, serverName);
      this.bucketName = bucketName;
      this.artifactPaths = artifactPaths;
    }
  }
}
