/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.alert;

import static io.harness.cvng.alert.entities.AlertRuleAnomaly.AlertRuleAnomalyStatus.CLOSED;
import static io.harness.cvng.alert.entities.AlertRuleAnomaly.AlertRuleAnomalyStatus.OPEN;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.alert.entities.AlertRuleAnomaly;
import io.harness.cvng.alert.services.AlertRuleAnomalyService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.persistence.HPersistence;
import io.harness.reflection.ReflectionUtils;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AlertRuleAnomalyServiceImplTest extends CvNextGenTestBase {
  @Inject private HPersistence hPersistence;
  @Mock private Clock clock;
  @Inject private AlertRuleAnomalyService alertRuleAnomalyService;

  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String serviceIdentifier;
  String envIdentifier;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    accountId = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    serviceIdentifier = generateUuid();
    envIdentifier = generateUuid();

    clock = Clock.fixed(Instant.parse("2020-04-22T10:02:06Z"), ZoneOffset.UTC);
    FieldUtils.writeField(alertRuleAnomalyService, "clock", clock, true);

    FieldUtils.writeField(alertRuleAnomalyService, "hPersistence", hPersistence, true);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testAlertRuleOpenAnomaly() {
    String uuid = generateUuid();

    AlertRuleAnomaly alertRuleAnomalyData =
        createOpenAlertRuleAnomaly(uuid, accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier);

    hPersistence.save(alertRuleAnomalyData);

    AlertRuleAnomaly ruleAnomaly = alertRuleAnomalyService.openAnomaly(accountId, orgIdentifier, projectIdentifier,
        serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE);

    AlertRuleAnomaly retrievedRuleAnomaly = hPersistence.get(AlertRuleAnomaly.class, alertRuleAnomalyData.getUuid());
    assertThat(retrievedRuleAnomaly).isNotNull();
    assertThat(retrievedRuleAnomaly.getAccountId()).isEqualTo(ruleAnomaly.getAccountId());
    assertThat(retrievedRuleAnomaly.getOrgIdentifier()).isEqualTo(ruleAnomaly.getOrgIdentifier());
    assertThat(retrievedRuleAnomaly.getProjectIdentifier()).isEqualTo(ruleAnomaly.getProjectIdentifier());
    assertThat(retrievedRuleAnomaly.getServiceIdentifier()).isEqualTo(ruleAnomaly.getServiceIdentifier());
    assertThat(retrievedRuleAnomaly.getEnvIdentifier()).isEqualTo(ruleAnomaly.getEnvIdentifier());
    assertThat(retrievedRuleAnomaly.getAlertRuleAnomalyStatus()).isEqualTo(OPEN);
    assertThat(retrievedRuleAnomaly.getAnomalyStartTime()).isEqualTo(ruleAnomaly.getAnomalyStartTime());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testUpsertAddsAllFields() {
    String uuid = generateUuid();

    AlertRuleAnomaly alertRuleAnomalyData =
        createOpenAlertRuleAnomaly(uuid, accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier);

    hPersistence.save(alertRuleAnomalyData);

    alertRuleAnomalyService.openAnomaly(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
        CVMonitoringCategory.PERFORMANCE);
    List<AlertRuleAnomaly> anomalies = hPersistence.createQuery(AlertRuleAnomaly.class, excludeAuthority).asList();
    Set<String> nullableFields = Sets.newHashSet();
    anomalies.forEach(alertRuleAnomaly -> {
      List<Field> fields = ReflectionUtils.getAllDeclaredAndInheritedFields(AlertRuleAnomaly.class);
      fields.stream().filter(field -> !nullableFields.contains(field.getName())).forEach(field -> {
        try {
          field.setAccessible(true);
          assertThat(field.get(alertRuleAnomaly)).withFailMessage("field %s is null", field.getName()).isNotNull();
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      });
    });
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testAlertRuleOpenAnomalyFirstTimeCreated() {
    AlertRuleAnomaly ruleAnomaly = alertRuleAnomalyService.openAnomaly(accountId, orgIdentifier, projectIdentifier,
        serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE);

    List<AlertRuleAnomaly> retrievedRuleAnomaly = hPersistence.createQuery(AlertRuleAnomaly.class).asList();

    assertThat(retrievedRuleAnomaly).isNotNull();
    assertThat(retrievedRuleAnomaly.get(0).getAccountId()).isEqualTo(ruleAnomaly.getAccountId());
    assertThat(retrievedRuleAnomaly.get(0).getOrgIdentifier()).isEqualTo(ruleAnomaly.getOrgIdentifier());
    assertThat(retrievedRuleAnomaly.get(0).getProjectIdentifier()).isEqualTo(ruleAnomaly.getProjectIdentifier());
    assertThat(retrievedRuleAnomaly.get(0).getServiceIdentifier()).isEqualTo(ruleAnomaly.getServiceIdentifier());
    assertThat(retrievedRuleAnomaly.get(0).getEnvIdentifier()).isEqualTo(ruleAnomaly.getEnvIdentifier());
    assertThat(retrievedRuleAnomaly.get(0).getAlertRuleAnomalyStatus()).isEqualTo(OPEN);
    assertThat(retrievedRuleAnomaly.get(0).getCategory()).isEqualTo(CVMonitoringCategory.PERFORMANCE);
    assertThat(retrievedRuleAnomaly.get(0).getAnomalyStartTime()).isEqualTo(ruleAnomaly.getAnomalyStartTime());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testAlertRuleAnomalyUpdateLastNotificationSentAt() {
    String uuid = generateUuid();

    AlertRuleAnomaly alertRuleAnomalyData = AlertRuleAnomaly.builder()
                                                .uuid(uuid)
                                                .accountId(accountId)
                                                .orgIdentifier(orgIdentifier)
                                                .projectIdentifier(projectIdentifier)
                                                .serviceIdentifier(serviceIdentifier)
                                                .envIdentifier(envIdentifier)
                                                .alertRuleAnomalyStatus(OPEN)
                                                .category(CVMonitoringCategory.PERFORMANCE)
                                                .lastNotificationSentAt(clock.instant().minus(Duration.ofHours(2)))
                                                .build();

    hPersistence.save(alertRuleAnomalyData);

    alertRuleAnomalyService.updateLastNotificationSentAt(accountId, orgIdentifier, projectIdentifier, serviceIdentifier,
        envIdentifier, CVMonitoringCategory.PERFORMANCE);

    AlertRuleAnomaly retrievedRuleAnomaly = hPersistence.get(AlertRuleAnomaly.class, alertRuleAnomalyData.getUuid());
    assertThat(retrievedRuleAnomaly).isNotNull();
    assertThat(retrievedRuleAnomaly.getAccountId()).isEqualTo(retrievedRuleAnomaly.getAccountId());
    assertThat(retrievedRuleAnomaly.getOrgIdentifier()).isEqualTo(retrievedRuleAnomaly.getOrgIdentifier());
    assertThat(retrievedRuleAnomaly.getProjectIdentifier()).isEqualTo(retrievedRuleAnomaly.getProjectIdentifier());
    assertThat(retrievedRuleAnomaly.getServiceIdentifier()).isEqualTo(retrievedRuleAnomaly.getServiceIdentifier());
    assertThat(retrievedRuleAnomaly.getEnvIdentifier()).isEqualTo(retrievedRuleAnomaly.getEnvIdentifier());
    assertThat(retrievedRuleAnomaly.getAlertRuleAnomalyStatus()).isEqualTo(OPEN);
    assertThat(retrievedRuleAnomaly.getLastNotificationSentAt()).isEqualTo(clock.instant());
    assertThat(retrievedRuleAnomaly.getAnomalyStartTime()).isEqualTo(retrievedRuleAnomaly.getAnomalyStartTime());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testAlertRuleCloseAnomaly() {
    String uuid = generateUuid();

    AlertRuleAnomaly alertRuleAnomalyData =
        createOpenAlertRuleAnomaly(uuid, accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier);

    hPersistence.save(alertRuleAnomalyData);

    alertRuleAnomalyService.closeAnomaly(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier,
        CVMonitoringCategory.PERFORMANCE);

    AlertRuleAnomaly retrievedRuleAnomaly = hPersistence.get(AlertRuleAnomaly.class, alertRuleAnomalyData.getUuid());
    assertThat(retrievedRuleAnomaly).isNotNull();
    assertThat(retrievedRuleAnomaly.getAccountId()).isEqualTo(retrievedRuleAnomaly.getAccountId());
    assertThat(retrievedRuleAnomaly.getOrgIdentifier()).isEqualTo(retrievedRuleAnomaly.getOrgIdentifier());
    assertThat(retrievedRuleAnomaly.getProjectIdentifier()).isEqualTo(retrievedRuleAnomaly.getProjectIdentifier());
    assertThat(retrievedRuleAnomaly.getServiceIdentifier()).isEqualTo(retrievedRuleAnomaly.getServiceIdentifier());
    assertThat(retrievedRuleAnomaly.getEnvIdentifier()).isEqualTo(retrievedRuleAnomaly.getEnvIdentifier());
    assertThat(retrievedRuleAnomaly.getAlertRuleAnomalyStatus()).isEqualTo(CLOSED);
    assertThat(retrievedRuleAnomaly.getAnomalyStartTime()).isEqualTo(retrievedRuleAnomaly.getAnomalyStartTime());
  }

  private AlertRuleAnomaly createOpenAlertRuleAnomaly(String uuid, String accountId, String orgIdentifier,
      String projectIdentifier, String serviceIdentifier, String envIdentifier) {
    return AlertRuleAnomaly.builder()
        .uuid(uuid)
        .accountId(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .serviceIdentifier(serviceIdentifier)
        .envIdentifier(envIdentifier)
        .category(CVMonitoringCategory.PERFORMANCE)
        .anomalyStartTime(clock.instant())
        .alertRuleAnomalyStatus(OPEN)
        .lastNotificationSentAt(clock.instant())
        .build();
  }
}
