/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.HANTANG;

import static java.util.Collections.EMPTY_LIST;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ccm.config.CCMConfig;
import io.harness.ccm.config.CCMConfigYamlHandler;
import io.harness.rule.Owner;

import software.wings.beans.yaml.ChangeContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CE)
public class CCMConfigYamlHandlerTest extends CategoryTest {
  private boolean isCloudCostEnabled = true;
  private CCMConfig ccmConfig;
  private CCMConfig.Yaml ccmConfigYaml;

  CCMConfigYamlHandler ccmConfigYamlHandler = new CCMConfigYamlHandler();

  @Before
  public void setUp() {
    ccmConfig = CCMConfig.builder().cloudCostEnabled(isCloudCostEnabled).build();
    ccmConfigYaml = CCMConfig.Yaml.builder().continuousEfficiencyEnabled(isCloudCostEnabled).build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testToYaml() {
    CCMConfig.Yaml yaml = ccmConfigYamlHandler.toYaml(ccmConfig, "");
    assertThat(yaml).isEqualTo(ccmConfigYaml);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testUpsertFromYaml() {
    ChangeContext<CCMConfig.Yaml> changeContext =
        ChangeContext.Builder.aChangeContext().withYaml(ccmConfigYaml).build();
    CCMConfig upsertedCcmConfig = ccmConfigYamlHandler.upsertFromYaml(changeContext, EMPTY_LIST);
    assertThat(upsertedCcmConfig).isEqualTo(ccmConfig);
  }
}
