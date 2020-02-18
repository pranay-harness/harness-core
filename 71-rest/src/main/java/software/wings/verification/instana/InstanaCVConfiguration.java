package software.wings.verification.instana;

import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.instana.InstanaDataCollectionInfo;
import software.wings.service.impl.instana.InstanaTagFilter;
import software.wings.verification.CVConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FieldNameConstants(innerTypeName = "InstanaCVConfigurationKeys")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class InstanaCVConfiguration extends CVConfiguration {
  private List<InstanaTagFilter> tagFilters;

  public List<InstanaTagFilter> getTagFilters() {
    if (tagFilters == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(tagFilters);
  }
  @Override
  public CVConfiguration deepCopy() {
    InstanaCVConfiguration clonedConfig = new InstanaCVConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setTagFilters(getTagFilters());
    return clonedConfig;
  }

  @Override
  public DataCollectionInfoV2 toDataCollectionInfo() {
    Map<String, String> hostsMap = new HashMap<>();
    hostsMap.put("DUMMY_24_7_HOST", DEFAULT_GROUP_NAME);
    InstanaDataCollectionInfo instanaDataCollectionInfo =
        InstanaDataCollectionInfo.builder().tagFilters(this.getTagFilters()).hostsToGroupNameMap(hostsMap).build();
    fillDataCollectionInfoWithCommonFields(instanaDataCollectionInfo);
    return instanaDataCollectionInfo;
  }

  @Override
  public boolean isCVTaskBasedCollectionEnabled() {
    return true;
  }
  /**
   * The type Yaml.
   */
  @Data
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class InstanaCVConfigurationYaml extends CVConfigurationYaml {
    private List<InstanaTagFilter> tagFilters;
  }
}
