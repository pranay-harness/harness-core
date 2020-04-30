package io.harness.datahandler.models;

import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Id;
import software.wings.beans.FeatureFlag;

import java.util.Objects;
import java.util.Set;

@Data
@Builder
public class FeatureFlagBO {
  @Id private String uuid;
  private String name;
  private boolean enabled;
  private boolean obsolete;
  private Set<String> accountIds;
  private long lastUpdatedAt;

  public static FeatureFlagBO fromFeatureFlag(FeatureFlag featureFlag) {
    if (Objects.isNull(featureFlag)) {
      return null;
    }
    return FeatureFlagBO.builder()
        .accountIds(featureFlag.getAccountIds())
        .enabled(featureFlag.isEnabled())
        .name(featureFlag.getName())
        .lastUpdatedAt(featureFlag.getLastUpdatedAt())
        .uuid(featureFlag.getUuid())
        .obsolete(featureFlag.isObsolete())
        .build();
  }
}
