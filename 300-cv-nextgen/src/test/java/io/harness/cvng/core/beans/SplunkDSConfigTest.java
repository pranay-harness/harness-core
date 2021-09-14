/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.DSConfig.CVConfigUpdateResult;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.CVNextGenConstants;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SplunkDSConfigTest extends DSConfigTestBase {
  private SplunkDSConfig splunkDSConfig;

  @Before
  public void setup() {
    splunkDSConfig = new SplunkDSConfig();
    fillCommonFields(splunkDSConfig);
    splunkDSConfig.setEnvIdentifier(envIdentifier);
    splunkDSConfig.setQuery("exception");
    splunkDSConfig.setServiceInstanceIdentifier("host");
    splunkDSConfig.setEventType(CVNextGenConstants.ERRORS_PACK_IDENTIFIER);
    splunkDSConfig.setServiceIdentifier("harness");
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_whenNoConfigExist() {
    CVConfigUpdateResult cvConfigUpdateResult = splunkDSConfig.getCVConfigUpdateResult(Collections.emptyList());
    assertThat(cvConfigUpdateResult.getUpdated()).isEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();
    List<CVConfig> added = cvConfigUpdateResult.getAdded();
    assertThat(added.size()).isEqualTo(1);
    SplunkCVConfig splunkCVConfig = (SplunkCVConfig) cvConfigUpdateResult.getAdded().get(0);
    assertCommon(splunkCVConfig, splunkDSConfig);
    assertThat(splunkCVConfig.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(splunkCVConfig.getUuid()).isNull();
    assertThat(splunkCVConfig.getQuery()).isEqualTo(splunkDSConfig.getQuery());
    assertThat(splunkCVConfig.getCategory())
        .isEqualTo(CVMonitoringCategory.fromDisplayName(splunkDSConfig.getEventType()));
    assertThat(splunkCVConfig.getServiceInstanceIdentifier()).isEqualTo(splunkDSConfig.getServiceInstanceIdentifier());
    assertThat(splunkCVConfig.getServiceIdentifier()).isEqualTo(splunkDSConfig.getServiceIdentifier());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_forExistingConfig() {
    CVConfigUpdateResult cvConfigUpdateResult =
        splunkDSConfig.getCVConfigUpdateResult(Lists.newArrayList(getSplunkCVConfig()));
    assertThat(cvConfigUpdateResult.getAdded()).isEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();
    List<CVConfig> added = cvConfigUpdateResult.getUpdated();
    assertThat(added.size()).isEqualTo(1);
    SplunkCVConfig splunkCVConfig = (SplunkCVConfig) cvConfigUpdateResult.getUpdated().get(0);
    assertCommon(splunkCVConfig, splunkDSConfig);
    assertThat(splunkCVConfig.getUuid()).isNotNull();
    assertThat(splunkCVConfig.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(splunkCVConfig.getQuery()).isEqualTo(splunkDSConfig.getQuery());
    assertThat(splunkCVConfig.getCategory())
        .isEqualTo(CVMonitoringCategory.fromDisplayName(splunkDSConfig.getEventType()));
    assertThat(splunkCVConfig.getServiceInstanceIdentifier()).isEqualTo(splunkDSConfig.getServiceInstanceIdentifier());
    assertThat(splunkCVConfig.getServiceIdentifier()).isEqualTo(splunkDSConfig.getServiceIdentifier());
  }

  private SplunkCVConfig getSplunkCVConfig() {
    SplunkCVConfig splunkCVConfig = new SplunkCVConfig();
    fillCommonFields(splunkCVConfig);
    splunkCVConfig.setUuid(generateUuid());
    splunkCVConfig.setQuery("old query");
    splunkCVConfig.setServiceInstanceIdentifier("host123");
    splunkCVConfig.setCategory(CVMonitoringCategory.ERRORS);
    return splunkCVConfig;
  }
}
