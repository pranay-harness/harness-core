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

public class CVConfigTest extends CategoryTest {
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
  public void testValidate_whenAccountIdIsUndefined() {
    CVConfig cvConfig = createCVConfig();
    cvConfig.setAccountId(null);
    assertThatThrownBy(() -> cvConfig.validate())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("accountId should not be null");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testValidate_whenConnectorIdIsUndefined() {
    CVConfig cvConfig = createCVConfig();
    cvConfig.setConnectorId(null);
    assertThatThrownBy(() -> cvConfig.validate())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("connectorId should not be null");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testValidate_whenServiceIdentifierIsUndefined() {
    CVConfig cvConfig = createCVConfig();
    cvConfig.setServiceIdentifier(null);
    assertThatThrownBy(() -> cvConfig.validate())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("serviceIdentifier should not be null");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testValidate_whenEnvironmentIdentifierIsUndefined() {
    CVConfig cvConfig = createCVConfig();
    cvConfig.setEnvIdentifier(null);
    assertThatThrownBy(() -> cvConfig.validate())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("envIdentifier should not be null");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testValidate_whenProjectIdentifierIsUndefined() {
    CVConfig cvConfig = createCVConfig();
    cvConfig.setProjectIdentifier(null);
    assertThatThrownBy(() -> cvConfig.validate())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("projectIdentifier should not be null");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testValidate_whenGroupIdIsUndefined() {
    CVConfig cvConfig = createCVConfig();
    cvConfig.setGroupId(null);
    assertThatThrownBy(() -> cvConfig.validate())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("groupId should not be null");
  }

  private CVConfig createCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    fillCommon(cvConfig);
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier(serviceInstanceIdentifier);
    cvConfig.setBaseline(
        TimeRange.builder().startTime(Instant.now()).endTime(Instant.now().plus(10, ChronoUnit.DAYS)).build());
    return cvConfig;
  }

  private void fillCommon(CVConfig cvConfig) {
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
