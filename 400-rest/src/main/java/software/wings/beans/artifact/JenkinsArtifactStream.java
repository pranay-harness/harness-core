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

import static software.wings.beans.artifact.ArtifactStreamType.JENKINS;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
@JsonTypeName("JENKINS")
@Data
@EqualsAndHashCode(callSuper = false)
public class JenkinsArtifactStream extends ArtifactStream {
  @NotEmpty private String jobname;
  private List<String> artifactPaths;

  public JenkinsArtifactStream() {
    super(JENKINS.name());
  }

  @Builder
  public JenkinsArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, boolean metadataOnly, String jobname,
      List<String> artifactPaths, String accountId, Set<String> keywords, boolean sample) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, JENKINS.name(), sourceName,
        settingId, name, autoPopulate, serviceId, metadataOnly, accountId, keywords, sample);
    this.jobname = jobname;
    this.artifactPaths = artifactPaths;
  }

  @Override
  public String generateSourceName() {
    return getJobname();
  }

  @Override
  public String fetchArtifactDisplayName(String buildNo) {
    return format("%s_%s_%s", getJobname(), buildNo, new SimpleDateFormat(dateFormat).format(new Date()));
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes(FeatureFlagService featureFlagService) {
    return ArtifactStreamAttributes.builder()
        .artifactStreamType(getArtifactStreamType())
        .jobName(jobname)
        .artifactPaths(artifactPaths)
        .build();
  }

  @Override
  public void validateRequiredFields() {
    if (!isMetadataOnly() && isEmpty(artifactPaths)) {
      throw new InvalidRequestException("Please provide at least one artifact path for non-metadata only");
    }
    // for both metadata and non-metadata remove artifact path containing empty strings
    List<String> updatedArtifactPaths = new ArrayList<>();
    if (isNotEmpty(artifactPaths)) {
      for (String artifactPath : artifactPaths) {
        if (isNotEmpty(artifactPath.trim())) {
          updatedArtifactPaths.add(artifactPath);
        }
      }
    }
    setArtifactPaths(updatedArtifactPaths);
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
  public boolean checkIfStreamParameterized() {
    if (isNotEmpty(artifactPaths)) {
      return validateParameters(jobname, artifactPaths.get(0));
    } else {
      return validateParameters(jobname);
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ArtifactStream.Yaml {
    private String jobName;
    private List<String> artifactPaths;
    private boolean metadataOnly;

    @lombok.Builder
    public Yaml(
        String harnessApiVersion, String serverName, boolean metadataOnly, String jobName, List<String> artifactPaths) {
      super(JENKINS.name(), harnessApiVersion, serverName);
      this.jobName = jobName;
      this.artifactPaths = artifactPaths;
      this.metadataOnly = metadataOnly;
    }
  }
}
