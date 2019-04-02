package software.wings.service.impl.analysis;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.MongoDataStoreServiceImpl;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.analysis.LogLabelingService;

import java.util.List;
import java.util.Map;

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

  @Before
  public void setup() {
    accountId = generateUuid();
    serviceId = generateUuid();
    labelingService = new LogLabelingServiceImpl();
    dataStoreService = new MongoDataStoreServiceImpl(wingsPersistence);
    setInternalState(labelingService, "dataStoreService", dataStoreService);
    setInternalState(labelingService, "featureFlagService", featureFlagService);
    when(featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, accountId)).thenReturn(true);
  }

  @Test
  @Category(UnitTests.class)
  public void testLabelIgnoreFeedback() {
    // setup
    LogMLFeedbackRecord record =
        LogMLFeedbackRecord.builder().serviceId(serviceId).logMessage("This is a test msg").build();
    wingsPersistence.save(record);

    // execute
    LogMLFeedbackRecord labelRecord = labelingService.getIgnoreFeedbackToClassify(accountId, serviceId);

    // verify
    assertNotNull(labelRecord);
    assertEquals(record.getLogMessage(), labelRecord.getLogMessage());
  }

  @Test
  @Category(UnitTests.class)
  public void testLabelIgnoreFeedbackGetFirstNonLabeled() {
    // setup
    LogMLFeedbackRecord record1 =
        LogMLFeedbackRecord.builder().serviceId(serviceId).logMessage("This is a test msg").build();
    record1.setSupervisedLabel("labelA");
    wingsPersistence.save(record1);
    LogMLFeedbackRecord record2 =
        LogMLFeedbackRecord.builder().serviceId(serviceId).logMessage("This is second test msg").build();
    wingsPersistence.save(record2);

    // execute
    LogMLFeedbackRecord labelRecord = labelingService.getIgnoreFeedbackToClassify(accountId, serviceId);

    // verify
    assertNotNull(labelRecord);
    assertEquals(record2.getLogMessage(), labelRecord.getLogMessage());
  }

  @Test
  @Category(UnitTests.class)
  public void testLabelIgnoreFeedbackGetLabelSamples() {
    // setup
    LogMLFeedbackRecord record1 = LogMLFeedbackRecord.builder()
                                      .serviceId(serviceId)
                                      .clusterLabel(-1)
                                      .logMessage("This is a test msg - A")
                                      .build();
    record1.setSupervisedLabel("labelA");
    wingsPersistence.save(record1);
    LogMLFeedbackRecord record2 = LogMLFeedbackRecord.builder()
                                      .serviceId(serviceId)
                                      .clusterLabel(-2)
                                      .logMessage("This is a test msg - B")
                                      .build();
    record2.setSupervisedLabel("labelB");
    wingsPersistence.save(record2);
    LogMLFeedbackRecord record3 = LogMLFeedbackRecord.builder()
                                      .serviceId(serviceId)
                                      .clusterLabel(-3)
                                      .logMessage("This is a test msg - C")
                                      .build();
    record3.setSupervisedLabel("labelC");
    wingsPersistence.save(record3);

    // execute
    Map<String, List<LogMLFeedbackRecord>> labelRecordSamples =
        labelingService.getLabeledSamplesForIgnoreFeedback(accountId, serviceId);

    // verify
    assertNotNull(labelRecordSamples);
    assertTrue(labelRecordSamples.containsKey("labelA"));
    assertTrue(labelRecordSamples.containsKey("labelB"));
    assertTrue(labelRecordSamples.containsKey("labelC"));
    assertEquals(record1.getLogMessage(), labelRecordSamples.get("labelA").get(0).getLogMessage());
    assertEquals(record2.getLogMessage(), labelRecordSamples.get("labelB").get(0).getLogMessage());
    assertEquals(record3.getLogMessage(), labelRecordSamples.get("labelC").get(0).getLogMessage());
  }

  @Test
  @Category(UnitTests.class)
  public void testSaveLabelIgnoreFeedback() {
    LogMLFeedbackRecord record =
        LogMLFeedbackRecord.builder().serviceId(serviceId).logMessage("This is a test msg").build();
    wingsPersistence.save(record);

    // execute
    labelingService.saveLabeledIgnoreFeedback(accountId, record, "labelA");

    // verify
    LogMLFeedbackRecord recordFromDB =
        wingsPersistence.createQuery(LogMLFeedbackRecord.class).filter("serviceId", serviceId).get();
    assertEquals("labelA", recordFromDB.getSupervisedLabel());
  }
}
