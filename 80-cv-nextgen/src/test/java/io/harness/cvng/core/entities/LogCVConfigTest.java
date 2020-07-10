package io.harness.cvng.core.entities;

import static io.harness.cvng.core.services.CVNextGenConstants.PERFORMANCE_PACK_IDENTIFIER;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.NEMANJA;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.TimeRange;
import io.harness.cvng.models.VerificationType;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class LogCVConfigTest extends CategoryTest {
  private String accountId;
  private String connectorId;
  private String productName;
  private String groupId;
  private String serviceInstanceIdentifier;

  @Before
  public void setup() {
    this.accountId = generateUuid();
    this.connectorId = generateUuid();
    this.productName = generateUuid();
    this.groupId = generateUuid();
    this.serviceInstanceIdentifier = generateUuid();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testValidateParams_whenQueryIsUndefined() {
    LogCVConfig logCVConfig = createCVConfig();
    assertThatThrownBy(() -> logCVConfig.validate())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("query should not be null");
  }

  private LogCVConfig createCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    fillCommon(cvConfig);
    cvConfig.setServiceInstanceIdentifier(serviceInstanceIdentifier);
    cvConfig.setBaseline(
        TimeRange.builder().startTime(Instant.now()).endTime(Instant.now().plus(10, ChronoUnit.DAYS)).build());
    return cvConfig;
  }

  private void fillCommon(LogCVConfig cvConfig) {
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
