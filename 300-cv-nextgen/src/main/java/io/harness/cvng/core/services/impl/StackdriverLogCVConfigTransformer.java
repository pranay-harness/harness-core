/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.cvng.beans.stackdriver.StackdriverLogDefinition;
import io.harness.cvng.core.beans.StackdriverLogDSConfig;
import io.harness.cvng.core.beans.StackdriverLogDSConfig.StackdriverLogConfiguration;
import io.harness.cvng.core.entities.StackdriverLogCVConfig;
import io.harness.cvng.core.services.api.CVConfigTransformer;

import java.util.ArrayList;
import java.util.List;

public class StackdriverLogCVConfigTransformer
    implements CVConfigTransformer<StackdriverLogCVConfig, StackdriverLogDSConfig> {
  @Override
  public StackdriverLogDSConfig transformToDSConfig(List<StackdriverLogCVConfig> cvConfigGroup) {
    if (isEmpty(cvConfigGroup)) {
      return null;
    }
    StackdriverLogDSConfig dsConfig = new StackdriverLogDSConfig();
    dsConfig.populateCommonFields(cvConfigGroup.get(0));

    List<StackdriverLogConfiguration> logConfigurations = new ArrayList<>();
    cvConfigGroup.forEach(config -> {
      StackdriverLogDefinition logDefinition = StackdriverLogDefinition.builder()
                                                   .name(config.getQueryName())
                                                   .query(config.getQuery())
                                                   .messageIdentifier(config.getMessageIdentifier())
                                                   .serviceInstanceIdentifier(config.getServiceInstanceIdentifier())
                                                   .build();
      logConfigurations.add(StackdriverLogConfiguration.builder()
                                .serviceIdentifier(config.getServiceIdentifier())
                                .envIdentifier(config.getEnvIdentifier())
                                .logDefinition(logDefinition)
                                .build());
    });
    dsConfig.setLogConfigurations(logConfigurations);
    return dsConfig;
  }
}
