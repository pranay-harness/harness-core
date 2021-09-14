/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.SplunkCVConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
@Data
@JsonTypeName("SPLUNK")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SplunkDSConfig extends DSConfig {
  private String query;
  private String serviceInstanceIdentifier;
  private String eventType;
  private String serviceIdentifier;
  private String envIdentifier;
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

  @Override
  public void validate(List<CVConfig> existingMapping) {
    // do nothing
  }

  private SplunkCVConfig toCVConfig() {
    SplunkCVConfig splunkCVConfig = new SplunkCVConfig();
    fillCommonFields(splunkCVConfig);
    splunkCVConfig.setQuery(this.query);
    splunkCVConfig.setServiceInstanceIdentifier(this.serviceInstanceIdentifier);
    splunkCVConfig.setCategory(CVMonitoringCategory.fromDisplayName(eventType));
    splunkCVConfig.setServiceIdentifier(serviceIdentifier);
    splunkCVConfig.setEnvIdentifier(envIdentifier);
    return splunkCVConfig;
  }
}
