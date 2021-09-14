/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import static io.harness.cvng.beans.CVMonitoringCategory.ERRORS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.stackdriver.StackdriverLogDefinition;
import io.harness.cvng.core.beans.DSConfig.CVConfigUpdateResult;
import io.harness.cvng.core.beans.StackdriverLogDSConfig.StackdriverLogConfiguration;
import io.harness.cvng.core.entities.StackdriverLogCVConfig;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StackdriverLogDSConfigTest extends DSConfigTestBase {
  private StackdriverLogDSConfig dsConfig;
  String serviceIdentifier;
  String envIdentifier;

  @Before
  public void setup() {
    dsConfig = new StackdriverLogDSConfig();
    fillCommonFields(dsConfig);
    envIdentifier = generateUuid();
    serviceIdentifier = generateUuid();
  }

  private StackdriverLogConfiguration getConfiguration(String query, String message) {
    return StackdriverLogConfiguration.builder()
        .envIdentifier(envIdentifier)
        .serviceIdentifier(serviceIdentifier)
        .logDefinition(StackdriverLogDefinition.builder()
                           .name(query)
                           .query(query)
                           .messageIdentifier(message)
                           .serviceInstanceIdentifier("pod_name")
                           .build())
        .build();
  }

  private StackdriverLogConfiguration getConfiguration(String query) {
    return getConfiguration(query, "message");
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
  public void testGetCVConfigUpdateResult_whenNoConfigExists() {
    List<StackdriverLogConfiguration> logConfigurations =
        Lists.newArrayList(getConfiguration("query1"), getConfiguration("query2"));
    dsConfig.setLogConfigurations(logConfigurations);
    CVConfigUpdateResult cvConfigUpdateResult = dsConfig.getCVConfigUpdateResult(Collections.emptyList());

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();

    assertThat(cvConfigUpdateResult.getAdded().size()).isEqualTo(2);

    List<StackdriverLogCVConfig> cvConfigs = (List<StackdriverLogCVConfig>) (List<?>) cvConfigUpdateResult.getAdded();
    cvConfigs.forEach(cvConfig -> {
      assertThat(cvConfig.getQueryName()).startsWith("query");
      assertThat(cvConfig.getQuery()).startsWith("query");
      assertThat(cvConfig.getMessageIdentifier()).isEqualTo("message");
      assertThat(cvConfig.getServiceIdentifier()).isEqualTo(serviceIdentifier);
      assertThat(cvConfig.getEnvIdentifier()).isEqualTo(envIdentifier);
      assertThat(cvConfig.getCategory().name()).isEqualTo(CVMonitoringCategory.ERRORS.name());
    });
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetCVConfigUpdateResult_withDeletions() {
    List<StackdriverLogConfiguration> logConfigurations = Lists.newArrayList(getConfiguration("query3"));
    dsConfig.setLogConfigurations(logConfigurations);
    CVConfigUpdateResult cvConfigUpdateResult =
        dsConfig.getCVConfigUpdateResult(Lists.newArrayList(getCVConfig("query1"), getCVConfig("query2")));

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isNotEmpty();

    assertThat(cvConfigUpdateResult.getAdded().size()).isEqualTo(1);
    assertThat(cvConfigUpdateResult.getDeleted().size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetCVConfigUpdateResult_withUpdates() {
    List<StackdriverLogConfiguration> logConfigurations =
        Lists.newArrayList(getConfiguration("query1", "new_message"), getConfiguration("query2"));
    dsConfig.setLogConfigurations(logConfigurations);
    CVConfigUpdateResult cvConfigUpdateResult =
        dsConfig.getCVConfigUpdateResult(Lists.newArrayList(getCVConfig("query1"), getCVConfig("query2")));

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();

    assertThat(cvConfigUpdateResult.getUpdated().size()).isEqualTo(2);

    List<StackdriverLogCVConfig> cvConfigs = (List<StackdriverLogCVConfig>) (List<?>) cvConfigUpdateResult.getUpdated();
    cvConfigs.forEach(cvConfig -> {
      assertThat(cvConfig.getQueryName()).startsWith("query");
      assertThat(cvConfig.getQuery()).startsWith("query");
      assertThat(cvConfig.getServiceIdentifier()).isEqualTo(serviceIdentifier);
      assertThat(cvConfig.getEnvIdentifier()).isEqualTo(envIdentifier);
      assertThat(cvConfig.getCategory().name()).isEqualTo(CVMonitoringCategory.ERRORS.name());
    });
    assertThat(cvConfigs.stream().map(cvConfig -> cvConfig.getMessageIdentifier()))
        .containsExactlyInAnyOrder("new_message", "message");
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetCVConfigUpdateResult_whenAddedToExisting() {
    List<StackdriverLogConfiguration> logConfigurations =
        Lists.newArrayList(getConfiguration("query1"), getConfiguration("query2"), getConfiguration("query3"));
    dsConfig.setLogConfigurations(logConfigurations);
    CVConfigUpdateResult cvConfigUpdateResult =
        dsConfig.getCVConfigUpdateResult(Lists.newArrayList(getCVConfig("query1"), getCVConfig("query2")));

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();

    assertThat(cvConfigUpdateResult.getUpdated().size()).isEqualTo(2);
    assertThat(cvConfigUpdateResult.getAdded().size()).isEqualTo(1);

    List<StackdriverLogCVConfig> cvConfigs = (List<StackdriverLogCVConfig>) (List<?>) cvConfigUpdateResult.getUpdated();
    cvConfigs.forEach(cvConfig -> {
      assertThat(cvConfig.getQueryName()).startsWith("query");
      assertThat(cvConfig.getQuery()).startsWith("query");
      assertThat(cvConfig.getServiceIdentifier()).isEqualTo(serviceIdentifier);
      assertThat(cvConfig.getEnvIdentifier()).isEqualTo(envIdentifier);
      assertThat(cvConfig.getCategory().name()).isEqualTo(CVMonitoringCategory.ERRORS.name());
    });
    cvConfigs = (List<StackdriverLogCVConfig>) (List<?>) cvConfigUpdateResult.getAdded();
    cvConfigs.forEach(cvConfig -> {
      assertThat(cvConfig.getQueryName()).startsWith("query");
      assertThat(cvConfig.getQuery()).startsWith("query");
      assertThat(cvConfig.getServiceIdentifier()).isEqualTo(serviceIdentifier);
      assertThat(cvConfig.getEnvIdentifier()).isEqualTo(envIdentifier);
      assertThat(cvConfig.getCategory().name()).isEqualTo(CVMonitoringCategory.ERRORS.name());
    });
  }
}
