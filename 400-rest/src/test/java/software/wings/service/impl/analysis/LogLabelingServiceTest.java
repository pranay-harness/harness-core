/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.MongoDataStoreServiceImpl;
import software.wings.service.impl.analysis.CVFeedbackRecord.CVFeedbackRecordKeys;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.analysis.LogLabelingService;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

/**
 * @author Praveen
 */
public class LogLabelingServiceTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  private LogLabelingService labelingService;
  private DataStoreService dataStoreService;
  @Mock private FeatureFlagService featureFlagService;

  private String accountId;
  private String serviceId;
  private String envId;

  @Before
  public void setup() throws IllegalAccessException {
    accountId = generateUuid();
    serviceId = generateUuid();
    envId = generateUuid();
    labelingService = new LogLabelingServiceImpl();
    dataStoreService = new MongoDataStoreServiceImpl(wingsPersistence);
    FieldUtils.writeField(labelingService, "dataStoreService", dataStoreService, true);
    FieldUtils.writeField(labelingService, "featureFlagService", featureFlagService, true);
    when(featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, accountId)).thenReturn(true);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testLabelIgnoreFeedback() {
    // setup
    CVFeedbackRecord record =
        CVFeedbackRecord.builder().serviceId(serviceId).envId(envId).logMessage("This is a test msg").build();
    wingsPersistence.save(record);

    // execute
    List<CVFeedbackRecord> labelRecord = labelingService.getCVFeedbackToClassify(accountId, serviceId);

    // verify
    assertThat(labelRecord).isNotNull();
    assertThat(labelRecord.get(0).getLogMessage()).isEqualTo(record.getLogMessage());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testLabelIgnoreFeedbackGetFirstNonLabeled() {
    // setup
    CVFeedbackRecord record1 =
        CVFeedbackRecord.builder().serviceId(serviceId).envId(envId).logMessage("This is a test msg").build();
    record1.setSupervisedLabel("labelA");
    wingsPersistence.save(record1);
    CVFeedbackRecord record2 =
        CVFeedbackRecord.builder().serviceId(serviceId).envId(envId).logMessage("This is second test msg").build();
    wingsPersistence.save(record2);

    // execute
    List<CVFeedbackRecord> labelRecord = labelingService.getCVFeedbackToClassify(accountId, serviceId);

    // verify
    assertThat(labelRecord).isNotNull();
    assertThat(labelRecord.get(0).getLogMessage()).isEqualTo(record2.getLogMessage());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testLabelIgnoreFeedbackGetLabelSamples() {
    // setup
    CVFeedbackRecord record1 = CVFeedbackRecord.builder()
                                   .serviceId(serviceId)
                                   .envId(envId)
                                   .clusterLabel(-1)
                                   .logMessage("This is a test msg - A")
                                   .build();
    record1.setSupervisedLabel("labelA");
    wingsPersistence.save(record1);
    CVFeedbackRecord record2 = CVFeedbackRecord.builder()
                                   .serviceId(serviceId)
                                   .envId(envId)
                                   .clusterLabel(-2)
                                   .logMessage("This is a test msg - B")
                                   .build();
    record2.setSupervisedLabel("labelB");
    wingsPersistence.save(record2);
    CVFeedbackRecord record3 = CVFeedbackRecord.builder()
                                   .serviceId(serviceId)
                                   .envId(envId)
                                   .clusterLabel(-3)
                                   .logMessage("This is a test msg - C")
                                   .build();
    record3.setSupervisedLabel("labelC");
    wingsPersistence.save(record3);

    // execute
    Map<String, List<CVFeedbackRecord>> labelRecordSamples =
        labelingService.getLabeledSamplesForIgnoreFeedback(accountId, serviceId, envId);

    // verify
    assertThat(labelRecordSamples).isNotNull();
    assertThat(labelRecordSamples.containsKey("labelA")).isTrue();
    assertThat(labelRecordSamples.containsKey("labelB")).isTrue();
    assertThat(labelRecordSamples.containsKey("labelC")).isTrue();
    assertThat(labelRecordSamples.get("labelA").get(0).getLogMessage()).isEqualTo(record1.getLogMessage());
    assertThat(labelRecordSamples.get("labelB").get(0).getLogMessage()).isEqualTo(record2.getLogMessage());
    assertThat(labelRecordSamples.get("labelC").get(0).getLogMessage()).isEqualTo(record3.getLogMessage());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testSaveLabelIgnoreFeedback() {
    CVFeedbackRecord record =
        CVFeedbackRecord.builder().uuid("cvuuid").serviceId(serviceId).logMessage("This is a test msg").build();
    wingsPersistence.save(record);

    // execute
    labelingService.saveLabeledIgnoreFeedback(accountId, record, "labelA");

    // verify
    CVFeedbackRecord recordFromDB =
        wingsPersistence.createQuery(CVFeedbackRecord.class).filter(CVFeedbackRecordKeys.uuid, "cvuuid").get();
    assertThat(recordFromDB.getSupervisedLabel()).isEqualTo("labelA");
  }
}
