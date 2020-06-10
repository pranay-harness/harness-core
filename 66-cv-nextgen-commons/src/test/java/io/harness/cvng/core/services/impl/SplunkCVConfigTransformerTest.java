package io.harness.cvng.core.services.impl;

import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cvng.core.services.api.CVConfigTransformerTestBase;
import io.harness.cvng.core.services.entities.SplunkCVConfig;
import io.harness.cvng.models.SplunkDSConfig;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;

public class SplunkCVConfigTransformerTest extends CVConfigTransformerTestBase {
  @Inject private SplunkCVConfigTransformer splunkCVConfigTransformer;
  @Before
  public void setup() {
    super.setUp();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void transformToDSConfig_precondition() {
    assertThatThrownBy(() -> splunkCVConfigTransformer.transformToDSConfig(Collections.emptyList()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Splunk Config should be of size 1 since it's a one to one mapping.");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void transformToDSConfig_withSplunkCVConfig() {
    SplunkCVConfig splunkCVConfig = new SplunkCVConfig();
    fillCommonFields(splunkCVConfig);
    splunkCVConfig.setCategory("QA");
    splunkCVConfig.setQuery("exception");
    splunkCVConfig.setServiceInstanceIdentifier("host");
    SplunkDSConfig splunkDSConfig =
        splunkCVConfigTransformer.transformToDSConfig(Collections.singletonList(splunkCVConfig));
    assertThat(splunkDSConfig.getQuery()).isEqualTo("exception");
    assertThat(splunkDSConfig.getEventType()).isEqualTo("QA");
    assertThat(splunkDSConfig.getServiceInstanceIdentifier()).isEqualTo("host");
  }
}