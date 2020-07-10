package io.harness.cvng.core.entities;

import static io.harness.cvng.core.services.CVNextGenConstants.PERFORMANCE_PACK_IDENTIFIER;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.NEMANJA;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.models.VerificationType;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MetricCVConfigTest extends CategoryTest {
  private String accountId;
  private String connectorId;
  private String productName;
  private String groupId;

  @Before
  public void setup() {
    this.accountId = generateUuid();
    this.connectorId = generateUuid();
    this.productName = generateUuid();
    this.groupId = generateUuid();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testValidateParams_whenMetricPackIsUndefined() {
    MetricCVConfig metricCVConfig = createCVConfig();
    assertThatThrownBy(() -> metricCVConfig.validate())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("metricPack should not be null");
  }

  private MetricCVConfig createCVConfig() {
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    fillCommon(cvConfig);
    cvConfig.setApplicationName("Application Name");
    cvConfig.setTierName("Tier Name");
    return cvConfig;
  }

  private void fillCommon(MetricCVConfig cvConfig) {
    cvConfig.setName("cvConfigName-" + generateUuid());
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(accountId);
    cvConfig.setConnectorId(connectorId);
    cvConfig.setServiceIdentifier(generateUuid());
    cvConfig.setEnvIdentifier(generateUuid());
    cvConfig.setProjectIdentifier(generateUuid());
    cvConfig.setGroupId(groupId);
    cvConfig.setCategory(PERFORMANCE_PACK_IDENTIFIER);
    cvConfig.setProductName(productName);
  }
}
