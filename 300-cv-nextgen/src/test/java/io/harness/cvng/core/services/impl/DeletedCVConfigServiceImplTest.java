package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.NEMANJA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DeletedCVConfig;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.DeletedCVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class DeletedCVConfigServiceImplTest extends CvNextGenTest {
  @Inject private HPersistence hPersistence;
  @Mock private DataCollectionTaskService dataCollectionTaskService;
  @Inject private DeletedCVConfigService deletedCVConfigServiceWithMocks;
  @Inject private DeletedCVConfigService deletedCVConfigService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private CVConfigService cvConfigService;

  private String accountId;
  private String connectorId;
  private String productName;
  private String groupId;
  private String serviceInstanceIdentifier;

  @Before
  public void setup() throws IllegalAccessException {
    this.accountId = generateUuid();
    this.connectorId = generateUuid();
    this.productName = generateUuid();
    this.groupId = generateUuid();
    this.serviceInstanceIdentifier = generateUuid();
    FieldUtils.writeField(
        deletedCVConfigServiceWithMocks, "dataCollectionTaskService", dataCollectionTaskService, true);
  }

  private DeletedCVConfig save(DeletedCVConfig deletedCVConfig) {
    return deletedCVConfigService.save(deletedCVConfig);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testSave() {
    CVConfig cvConfig = createCVConfig();
    DeletedCVConfig saved = save(createDeletedCVConfig(cvConfig));
    assertThat(saved.getAccountId()).isEqualTo(cvConfig.getAccountId());
    assertThat(saved.getPerpetualTaskId()).isEqualTo(cvConfig.getPerpetualTaskId());
    assertThat(saved.getCvConfig()).isNotNull();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testTriggerCleanup() {
    CVConfig cvConfig = createCVConfig();
    DeletedCVConfig saved = save(createDeletedCVConfig(cvConfig));
    deletedCVConfigServiceWithMocks.triggerCleanup(saved);
    assertThat(hPersistence.get(DeletedCVConfig.class, saved.getUuid())).isNull();
    verify(dataCollectionTaskService, times(1)).deletePerpetualTasks(saved.getAccountId(), saved.getPerpetualTaskId());
    assertThatThrownBy(() -> verificationTaskService.getServiceGuardVerificationTaskId(accountId, cvConfig.getUuid()))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("VerificationTask mapping does not exist for cvConfigId " + cvConfig.getUuid()
            + ". Please check cvConfigId");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGet() {
    CVConfig cvConfig = createCVConfig();
    DeletedCVConfig updated = save(createDeletedCVConfig(cvConfig));
    DeletedCVConfig saved = deletedCVConfigService.get(updated.getUuid());
    assertThat(saved.getAccountId()).isEqualTo(updated.getCvConfig().getAccountId());
    assertThat(saved.getPerpetualTaskId()).isEqualTo(updated.getCvConfig().getPerpetualTaskId());
    assertThat(saved.getCvConfig()).isNotNull();
  }

  private CVConfig createCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    fillCommon(cvConfig);
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier(serviceInstanceIdentifier);
    cvConfigService.save(cvConfig);
    return cvConfig;
  }

  private DeletedCVConfig createDeletedCVConfig(CVConfig cvConfig) {
    return DeletedCVConfig.builder().accountId(cvConfig.getAccountId()).cvConfig(cvConfig).build();
  }

  private void fillCommon(CVConfig cvConfig) {
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(accountId);
    cvConfig.setConnectorIdentifier(connectorId);
    cvConfig.setServiceIdentifier(generateUuid());
    cvConfig.setEnvIdentifier(generateUuid());
    cvConfig.setProjectIdentifier(generateUuid());
    cvConfig.setGroupId(groupId);
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(productName);
  }
}
