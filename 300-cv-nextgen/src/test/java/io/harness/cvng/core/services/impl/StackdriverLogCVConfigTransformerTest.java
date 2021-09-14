/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import static io.harness.cvng.beans.CVMonitoringCategory.ERRORS;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.cvng.api.CVConfigTransformerTestBase;
import io.harness.cvng.core.beans.StackdriverLogDSConfig;
import io.harness.cvng.core.entities.StackdriverLogCVConfig;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StackdriverLogCVConfigTransformerTest extends CVConfigTransformerTestBase {
  @Inject private StackdriverLogCVConfigTransformer stackdriverLogCVConfigTransformer;

  @Before
  public void setup() {
    super.setUp();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void transformToDSConfig_precondition() {
    assertThatThrownBy(() -> stackdriverLogCVConfigTransformer.transform(Collections.emptyList()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("List of cvConfigs can not empty");
  }

  private StackdriverLogCVConfig getCVConfig(String query) {
    StackdriverLogCVConfig cvConfig = StackdriverLogCVConfig.builder().build();
    fillCommonFields(cvConfig);
    cvConfig.setEnvIdentifier(envIdentifier);
    cvConfig.setServiceIdentifier(serviceIdentifier);
    cvConfig.setCategory(ERRORS);
    cvConfig.setQueryName(query);
    cvConfig.setQuery(query);
    cvConfig.setMessageIdentifier("message");
    cvConfig.setServiceInstanceIdentifier("pod_name");
    return cvConfig;
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void transformToDSConfig_with1CVConfig() {
    StackdriverLogDSConfig dsConfig =
        stackdriverLogCVConfigTransformer.transformToDSConfig(Arrays.asList(getCVConfig("query1")));
    assertThat(dsConfig).isNotNull();
    assertThat(dsConfig.getLogConfigurations().size()).isEqualTo(1);
    assertThat(dsConfig.getLogConfigurations().get(0).getLogDefinition().getQuery()).isEqualTo("query1");
    assertThat(dsConfig.getLogConfigurations().get(0).getServiceIdentifier()).isEqualTo(serviceIdentifier);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void transformToDSConfig_withMultipleCVConfigs() {
    StackdriverLogDSConfig dsConfig = stackdriverLogCVConfigTransformer.transformToDSConfig(
        Lists.newArrayList(getCVConfig("query1"), getCVConfig("query2")));
    assertThat(dsConfig).isNotNull();
    assertThat(dsConfig.getLogConfigurations().size()).isEqualTo(2);
    dsConfig.getLogConfigurations().forEach(config -> {
      assertThat(config.getServiceIdentifier()).isEqualTo(serviceIdentifier);
      assertThat(config.getEnvIdentifier()).isEqualTo(envIdentifier);
      assertThat(config.getLogDefinition().getQuery()).startsWith("query");
    });
  }
}
