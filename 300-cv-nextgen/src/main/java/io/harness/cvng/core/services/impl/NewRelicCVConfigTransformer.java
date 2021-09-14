/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.beans.NewRelicDSConfig;
import io.harness.cvng.core.beans.NewRelicDSConfig.NewRelicServiceConfig;
import io.harness.cvng.core.entities.NewRelicCVConfig;
import io.harness.cvng.core.services.api.CVConfigTransformer;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;

public class NewRelicCVConfigTransformer implements CVConfigTransformer<NewRelicCVConfig, NewRelicDSConfig> {
  @Override
  public NewRelicDSConfig transformToDSConfig(List<NewRelicCVConfig> cvConfigGroup) {
    NewRelicDSConfig dsConfig = new NewRelicDSConfig();
    dsConfig.populateCommonFields(cvConfigGroup.get(0));
    // TODO: Fix the metric pack list part properly.
    List<NewRelicServiceConfig> newRelicServiceConfigList = new ArrayList<>();
    cvConfigGroup.forEach(newRelicCVConfig -> {
      newRelicServiceConfigList.add(NewRelicServiceConfig.builder()
                                        .applicationName(newRelicCVConfig.getApplicationName())
                                        .applicationId(newRelicCVConfig.getApplicationId())
                                        .envIdentifier(newRelicCVConfig.getEnvIdentifier())
                                        .serviceIdentifier(newRelicCVConfig.getServiceIdentifier())
                                        .metricPacks(Sets.newHashSet(newRelicCVConfig.getMetricPack()))
                                        .build());
    });
    dsConfig.setNewRelicServiceConfigList(newRelicServiceConfigList);
    return dsConfig;
  }
}
