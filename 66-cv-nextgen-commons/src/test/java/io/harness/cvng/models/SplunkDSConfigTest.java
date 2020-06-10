package io.harness.cvng.models;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;

import io.harness.category.element.UnitTests;
import io.harness.cvng.core.services.entities.CVConfig;
import io.harness.cvng.core.services.entities.SplunkCVConfig;
import io.harness.cvng.models.DSConfig.CVConfigUpdateResult;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;
import java.util.List;

public class SplunkDSConfigTest extends DSConfigTestBase {
  private SplunkDSConfig splunkDSConfig;

  @Before
  public void setup() {
    splunkDSConfig = new SplunkDSConfig();
    fillCommonFields(splunkDSConfig);
    splunkDSConfig.setQuery("exception");
    splunkDSConfig.setServiceInstanceIdentifier("host");
    splunkDSConfig.setEventType("QA");
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
    assertThat(splunkCVConfig.getUuid()).isNull();
    assertThat(splunkCVConfig.getQuery()).isEqualTo(splunkDSConfig.getQuery());
    assertThat(splunkCVConfig.getCategory()).isEqualTo(splunkDSConfig.getEventType());
    assertThat(splunkCVConfig.getServiceInstanceIdentifier()).isEqualTo(splunkDSConfig.getServiceInstanceIdentifier());
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
    assertThat(splunkCVConfig.getQuery()).isEqualTo(splunkDSConfig.getQuery());
    assertThat(splunkCVConfig.getCategory()).isEqualTo(splunkDSConfig.getEventType());
    assertThat(splunkCVConfig.getServiceInstanceIdentifier()).isEqualTo(splunkDSConfig.getServiceInstanceIdentifier());
  }

  private SplunkCVConfig getSplunkCVConfig() {
    SplunkCVConfig splunkCVConfig = new SplunkCVConfig();
    fillCommonFields(splunkCVConfig);
    splunkCVConfig.setUuid(generateUuid());
    splunkCVConfig.setQuery("old query");
    splunkCVConfig.setServiceInstanceIdentifier("host123");
    splunkCVConfig.setCategory("QA");
    return splunkCVConfig;
  }
}