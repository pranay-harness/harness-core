package io.harness.cvng;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.TimeRange;
import io.harness.cvng.core.services.entities.CVConfig;
import io.harness.cvng.core.services.entities.SplunkCVConfig;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CVConfigServiceImplTest extends CVNextGenBaseTest {
  @Inject CVConfigService cvConfigService;
  private String accountId;
  private String connectorId;
  @Before
  public void setup() {
    this.accountId = generateUuid();
    this.connectorId = generateUuid();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSave() {
    CVConfig cvConfig = createCVConfig();
    CVConfig updated = save(cvConfig);
    CVConfig saved = cvConfigService.get(updated.getUuid());
    assertCommons(saved, cvConfig);
  }

  private CVConfig save(CVConfig cvConfig) {
    return cvConfigService.save(cvConfig);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSave_batchAPI() {
    List<CVConfig> cvConfigs = createCVConfigs(5);
    save(cvConfigs);
    cvConfigs.forEach(cvConfig -> assertCommons(cvConfigService.get(cvConfig.getUuid()), cvConfig));
  }

  private List<CVConfig> save(List<CVConfig> cvConfigs) {
    return cvConfigService.save(cvConfigs);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSave_batchAPIIfUUIDIsDefined() {
    List<CVConfig> cvConfigs = createCVConfigs(5);
    cvConfigs.forEach(cvConfig -> cvConfig.setUuid(generateUuid()));
    assertThatThrownBy(() -> save(cvConfigs))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("UUID should be null when creating CVConfig");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGet() {
    CVConfig cvConfig = createCVConfig();
    CVConfig updated = save(cvConfig);
    CVConfig saved = cvConfigService.get(updated.getUuid());
    assertCommons(saved, cvConfig);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDelete() {
    CVConfig cvConfig = createCVConfig();
    CVConfig updated = save(cvConfig);
    cvConfigService.delete(updated.getUuid());
    assertThat(cvConfigService.get(cvConfig.getUuid())).isEqualTo(null);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDelete_batchAPI() {
    List<CVConfig> cvConfigs = createCVConfigs(3);
    cvConfigs.forEach(cvConfig -> save(cvConfig));
    cvConfigService.delete(cvConfigs.stream().map(cvConfig -> cvConfig.getUuid()).collect(Collectors.toList()));
    cvConfigs.forEach(cvConfig -> assertThat(cvConfigService.get(cvConfig.getUuid())).isEqualTo(null));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdate_withMultipleCVConfig() {
    CVConfig cvConfig = createCVConfig();
    save(cvConfig);
    CVConfig updated = cvConfigService.get(cvConfig.getUuid());
    updated.setName("this is updated config name");
    cvConfigService.update(Lists.newArrayList(updated));
    assertCommons(cvConfigService.get(updated.getUuid()), updated);
    assertThat(updated.getName()).isEqualTo("this is updated config name");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdate_withEmptyCVConfigId() {
    CVConfig cvConfig = createCVConfig();
    cvConfig.setName("this is updated config name");
    assertThatThrownBy(() -> cvConfigService.update(Lists.newArrayList(cvConfig)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Trying to update a CVConfig with empty UUID.");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testList_findSingleCVConfig() {
    CVConfig cvConfig = createCVConfig();
    save(cvConfig);
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, cvConfig.getConnectorId());
    assertCommons(cvConfigs.get(0), cvConfig);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testList_zeroMatch() {
    CVConfig cvConfig = createCVConfig();
    save(cvConfig);
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, generateUuid());
    assertThat(cvConfigs).hasSize(0);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testList_multipleMatchMultipleConnectorIds() {
    List<CVConfig> cvConfigs1 = createCVConfigs(5);
    String connectorId1 = generateUuid();
    cvConfigs1.forEach(cvConfig -> {
      cvConfig.setConnectorId(connectorId1);
      save(cvConfig);
    });

    List<CVConfig> cvConfigs2 = createCVConfigs(7);
    String connectorId2 = generateUuid();
    cvConfigs2.forEach(cvConfig -> {
      cvConfig.setConnectorId(connectorId2);
      save(cvConfig);
    });

    assertThat(cvConfigService.list(accountId, connectorId1)).hasSize(5);
    assertThat(cvConfigService.list(accountId, connectorId2)).hasSize(7);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetProjectsNames_whenNoConfigsPresent() {
    assertThat(cvConfigService.getProductNames(accountId, generateUuid())).isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetProjectsNames_withMultipleDuplicateProjectNames() {
    List<CVConfig> cvConfigs = createCVConfigs(5);
    List<String> projectNames = Arrays.asList("p2", "p1", "p2", "p3", "p3");
    IntStream.range(0, 5).forEach(index -> cvConfigs.get(index).setProductName(projectNames.get(index)));
    save(cvConfigs);
    assertThat(cvConfigService.getProductNames(accountId, connectorId)).isEqualTo(Lists.newArrayList("p1", "p2", "p3"));
  }

  private void assertCommons(CVConfig actual, CVConfig expected) {
    assertThat(actual.getType()).isEqualTo(expected.getType());
    assertThat(actual.getAccountId()).isEqualTo(expected.getAccountId());
    assertThat(actual.getCategory()).isEqualTo(expected.getCategory());
    assertThat(actual.getConnectorId()).isEqualTo(expected.getConnectorId());
    assertThat(actual.getEnvId()).isEqualTo(expected.getEnvId());
    assertThat(actual.getServiceId()).isEqualTo(expected.getServiceId());
    assertThat(actual.getName()).isEqualTo(expected.getName());
  }

  public List<CVConfig> createCVConfigs(int n) {
    return IntStream.range(0, n).mapToObj(index -> createCVConfig()).collect(Collectors.toList());
  }

  private CVConfig createCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    fillCommon(cvConfig);
    cvConfig.setQuery("exception");
    cvConfig.setBaseline(
        TimeRange.builder().startTime(Instant.now()).endTime(Instant.now().plus(10, ChronoUnit.DAYS)).build());
    return cvConfig;
  }

  private void fillCommon(CVConfig cvConfig) {
    cvConfig.setConnectorId(connectorId);
    cvConfig.setCategory("Performance");
    cvConfig.setAccountId(accountId);
    cvConfig.setEnvId(generateUuid());
    cvConfig.setName("cvConfigName-" + generateUuid());
    cvConfig.setProjectId(generateUuid());
  }
}