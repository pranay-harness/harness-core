/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.KANHAIYA;
import static io.harness.rule.OwnerRule.NEMANJA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.VerificationApplication;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.LogRecordDTO;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DeletedCVConfig;
import io.harness.cvng.core.entities.LogRecord;
import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask;
import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask.MonitoringSourcePerpetualTaskKeys;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DeletedCVConfigService;
import io.harness.cvng.core.services.api.LogRecordService;
import io.harness.cvng.core.services.api.MonitoringSourcePerpetualTaskService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentEntity;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

public class DeletedCVConfigServiceImplTest extends CvNextGenTestBase {
  @Inject private HPersistence hPersistence;
  @Inject private DeletedCVConfigService deletedCVConfigServiceWithMocks;
  @Inject private DeletedCVConfigService deletedCVConfigService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private CVConfigService cvConfigService;
  @Inject private LogRecordService logRecordService;
  @Inject private MonitoringSourcePerpetualTaskService monitoringSourcePerpetualTaskService;

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
  }

  private DeletedCVConfig save(DeletedCVConfig deletedCVConfig) {
    return deletedCVConfigService.save(deletedCVConfig, Duration.ZERO);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testSave() {
    CVConfig cvConfig = createCVConfig();
    DeletedCVConfig saved = save(createDeletedCVConfig(cvConfig));
    assertThat(saved.getAccountId()).isEqualTo(cvConfig.getAccountId());
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
    assertThatThrownBy(() -> verificationTaskService.getServiceGuardVerificationTaskId(accountId, cvConfig.getUuid()))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("VerificationTask mapping does not exist for cvConfigId " + cvConfig.getUuid()
            + ". Please check cvConfigId");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testTriggerCleanup_deleteVerificationTask() {
    CVConfig cvConfig = createCVConfig();
    String deploymentVerficationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfig.getUuid(), generateUuid(), DataSourceType.APP_DYNAMICS);
    DeletedCVConfig saved = save(createDeletedCVConfig(cvConfig));
    List<String> verificationTaskIds =
        verificationTaskService.getServiceGuardVerificationTaskIds(accountId, cvConfig.getUuid());

    LogRecordDTO logRecord1 = LogRecordDTO.builder()
                                  .verificationTaskId(verificationTaskIds.get(0))
                                  .accountId(accountId)
                                  .timestamp(Instant.now().toEpochMilli())
                                  .log("log message")
                                  .host("host")
                                  .build();
    LogRecordDTO logRecord2 = LogRecordDTO.builder()
                                  .verificationTaskId(deploymentVerficationTaskId)
                                  .accountId(accountId)
                                  .timestamp(Instant.now().toEpochMilli())
                                  .log("log message")
                                  .host("host")
                                  .build();
    logRecordService.save(Arrays.asList(logRecord1, logRecord2));
    deletedCVConfigServiceWithMocks.triggerCleanup(saved);
    List<LogRecord> logRecords = hPersistence.createQuery(LogRecord.class).asList();
    assertThat(logRecords).hasSize(1);
    assertThat(logRecords.get(0).getVerificationTaskId()).isEqualTo(deploymentVerficationTaskId);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testTriggerCleanup_deleteMonitoringSourcePerpetualTasks() {
    CVConfig cvConfig = createCVConfig();
    DeletedCVConfig saved = save(createDeletedCVConfig(cvConfig));
    monitoringSourcePerpetualTaskService.createTask(accountId, cvConfig.getOrgIdentifier(),
        cvConfig.getProjectIdentifier(), cvConfig.getConnectorIdentifier(), cvConfig.getIdentifier(), false);
    List<MonitoringSourcePerpetualTask> monitoringSourcePerpetualTasks =
        hPersistence.createQuery(MonitoringSourcePerpetualTask.class)
            .filter(MonitoringSourcePerpetualTaskKeys.monitoringSourceIdentifier, cvConfig.getIdentifier())
            .asList();
    assertThat(monitoringSourcePerpetualTasks).hasSize(2);
    deletedCVConfigServiceWithMocks.triggerCleanup(saved);
    monitoringSourcePerpetualTasks =
        hPersistence.createQuery(MonitoringSourcePerpetualTask.class)
            .filter(MonitoringSourcePerpetualTaskKeys.monitoringSourceIdentifier, cvConfig.getIdentifier())
            .asList();
    assertThat(monitoringSourcePerpetualTasks).hasSize(0);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testTriggerCleanup_entitiesList() {
    Set<Class<? extends PersistentEntity>> entitiesWithVerificationTaskId = new HashSet<>();
    entitiesWithVerificationTaskId.addAll(DeletedCVConfigServiceImpl.ENTITIES_TO_DELETE_BY_VERIFICATION_ID);
    entitiesWithVerificationTaskId.addAll(DeletedCVConfigServiceImpl.ENTITIES_DELETE_BLACKLIST_BY_VERIFICATION_ID);
    Reflections reflections = new Reflections(VerificationApplication.class.getPackage().getName());
    Set<Class<? extends PersistentEntity>> withVerificationTaskId = new HashSet<>();
    reflections.getSubTypesOf(PersistentEntity.class).forEach(entity -> {
      if (doesClassContainField(entity, VerificationTask.VERIFICATION_TASK_ID_KEY)) {
        withVerificationTaskId.add(entity);
      }
    });
    assertThat(entitiesWithVerificationTaskId)
        .isEqualTo(withVerificationTaskId)
        .withFailMessage(
            "Entities with verificationTaskId found which is not added to ENTITIES_TO_DELETE_BY_VERIFICATION_ID or ENTITIES_DELETE_BLACKLIST_BY_VERIFICATION_ID");
  }

  private boolean doesClassContainField(Class<?> clazz, String fieldName) {
    return Arrays.stream(clazz.getDeclaredFields()).anyMatch(f -> f.getName().equals(fieldName));
  }
  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGet() {
    CVConfig cvConfig = createCVConfig();
    DeletedCVConfig updated = save(createDeletedCVConfig(cvConfig));
    DeletedCVConfig saved = deletedCVConfigService.get(updated.getUuid());
    assertThat(saved.getAccountId()).isEqualTo(updated.getCvConfig().getAccountId());
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
    cvConfig.setOrgIdentifier(generateUuid());
    cvConfig.setEnvIdentifier(generateUuid());
    cvConfig.setProjectIdentifier(generateUuid());
    cvConfig.setIdentifier(groupId);
    cvConfig.setMonitoringSourceName(generateUuid());
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(productName);
  }
}
