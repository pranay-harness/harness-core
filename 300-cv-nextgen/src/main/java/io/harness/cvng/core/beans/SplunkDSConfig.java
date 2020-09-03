package io.harness.cvng.core.beans;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.SplunkCVConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@JsonTypeName("SPLUNK")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SplunkDSConfig extends DSConfig {
  private String query;
  private String serviceInstanceIdentifier;
  private String eventType;
  private String serviceIdentifier;
  @Override
  public DataSourceType getType() {
    return DataSourceType.SPLUNK;
  }

  @Override
  public CVConfigUpdateResult getCVConfigUpdateResult(List<CVConfig> existingCVConfigs) {
    List<CVConfig> updatedConfig = Lists.newArrayList(toCVConfig());
    if (existingCVConfigs.isEmpty()) {
      return CVConfigUpdateResult.builder().added(updatedConfig).build();
    } else {
      Preconditions.checkArgument(existingCVConfigs.size() == 1,
          "Size can not be greater then one for splunk config. It is a one to one mapping.");
      updatedConfig.get(0).setUuid(existingCVConfigs.get(0).getUuid());
      return CVConfigUpdateResult.builder().updated(updatedConfig).build();
    }
  }

  private SplunkCVConfig toCVConfig() {
    SplunkCVConfig splunkCVConfig = new SplunkCVConfig();
    fillCommonFields(splunkCVConfig);
    splunkCVConfig.setQuery(this.query);
    splunkCVConfig.setServiceInstanceIdentifier(this.serviceInstanceIdentifier);
    splunkCVConfig.setCategory(CVMonitoringCategory.fromDisplayName(eventType));
    splunkCVConfig.setServiceIdentifier(serviceIdentifier);
    return splunkCVConfig;
  }
}
