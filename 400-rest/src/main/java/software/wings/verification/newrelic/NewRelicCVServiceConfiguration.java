package software.wings.verification.newrelic;

import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import io.harness.beans.FeatureName;

import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfoV2;
import software.wings.verification.CVConfiguration;

import com.github.reinert.jjschema.Attributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author Vaibhav Tulsyan
 * 05/Oct/2018
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NewRelicCVServiceConfiguration extends CVConfiguration {
  @Attributes(required = true, title = "Application Name") private String applicationId;
  @Attributes(required = true, title = "Metrics") private List<String> metrics;

  @Override
  public CVConfiguration deepCopy() {
    NewRelicCVServiceConfiguration clonedConfig = new NewRelicCVServiceConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setApplicationId(this.getApplicationId());
    clonedConfig.setMetrics(this.getMetrics());
    return clonedConfig;
  }

  @Override
  public DataCollectionInfoV2 toDataCollectionInfo() {
    Map<String, String> hostsMap = new HashMap<>();
    hostsMap.put("DUMMY_24_7_HOST", DEFAULT_GROUP_NAME);
    NewRelicDataCollectionInfoV2 newRelicDataCollectionInfoV2 =
        NewRelicDataCollectionInfoV2.builder()
            .newRelicAppId(Long.parseLong(this.getApplicationId()))
            .hostsToGroupNameMap(hostsMap)
            .build();
    fillDataCollectionInfoWithCommonFields(newRelicDataCollectionInfoV2);
    return newRelicDataCollectionInfoV2;
  }

  @Override
  public boolean isCVTaskBasedCollectionFeatureFlagged() {
    return true;
  }

  @Override
  public FeatureName getCVTaskBasedCollectionFeatureFlag() {
    return FeatureName.NEWRELIC_24_7_CV_TASK;
  }
}
