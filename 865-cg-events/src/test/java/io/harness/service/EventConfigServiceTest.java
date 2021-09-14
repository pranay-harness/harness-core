/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.DEEPAK_PUTHRAYA;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CgEventConfig;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Slf4j
@OwnedBy(CDC)
public class EventConfigServiceTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  public static final String GLOBAL_APP_ID = "__GLOBAL_APP_ID__";
  public static final String GLOBAL_ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";

  @Mock private HPersistence hPersistence;
  @InjectMocks private EventConfigServiceImpl eventConfigService;

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void createEventsConfigAllValidations() {
    CgEventConfig cgEventConfig = new CgEventConfig();
    cgEventConfig.setName("Event Name");
    Assertions
        .assertThatThrownBy(
            () -> eventConfigService.createEventsConfig(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID, cgEventConfig))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Event config requires rule to be specified");
  }
}
