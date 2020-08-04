package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.NEMANJA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.core.beans.CVMonitoringCategory;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DeletedCVConfig;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.DeletedCVConfigService;
import io.harness.cvng.models.VerificationType;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DeletedCVConfigServiceImplTest extends CvNextGenTest {
  @Mock private HPersistence hPersistence;
  @Mock private DataCollectionTaskService dataCollectionTaskService;
  @InjectMocks private DeletedCVConfigService deletedCVConfigServiceWithMocks = new DeletedCVConfigServiceImpl();
  @Inject private DeletedCVConfigService deletedCVConfigService;

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
    assertThat(saved.getDataCollectionTaskId()).isEqualTo(cvConfig.getDataCollectionTaskId());
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
    verify(dataCollectionTaskService, times(1))
        .deleteDataCollectionTask(saved.getAccountId(), saved.getDataCollectionTaskId());
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGet() {
    CVConfig cvConfig = createCVConfig();
    DeletedCVConfig updated = save(createDeletedCVConfig(cvConfig));
    DeletedCVConfig saved = deletedCVConfigService.get(updated.getUuid());
    assertThat(saved.getAccountId()).isEqualTo(updated.getCvConfig().getAccountId());
    assertThat(saved.getDataCollectionTaskId()).isEqualTo(updated.getCvConfig().getDataCollectionTaskId());
    assertThat(saved.getCvConfig()).isNotNull();
  }

  private CVConfig createCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    fillCommon(cvConfig);
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier(serviceInstanceIdentifier);
    return cvConfig;
  }

  private DeletedCVConfig createDeletedCVConfig(CVConfig cvConfig) {
    return DeletedCVConfig.builder().accountId(cvConfig.getAccountId()).cvConfig(cvConfig).build();
  }

  private void fillCommon(CVConfig cvConfig) {
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(accountId);
    cvConfig.setConnectorId(connectorId);
    cvConfig.setServiceIdentifier(generateUuid());
    cvConfig.setEnvIdentifier(generateUuid());
    cvConfig.setProjectIdentifier(generateUuid());
    cvConfig.setGroupId(groupId);
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(productName);
  }
}
