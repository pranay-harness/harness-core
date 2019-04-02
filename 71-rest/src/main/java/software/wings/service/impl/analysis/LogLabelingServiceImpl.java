package software.wings.service.impl.analysis;

import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.serializer.JsonUtils;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.GoogleDataStoreServiceImpl;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.analysis.LogLabelingService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Singleton
public class LogLabelingServiceImpl implements LogLabelingService {
  private static final Logger logger = LoggerFactory.getLogger(LogLabelingServiceImpl.class);
  private static final int MAX_CASSIFICATION_COUNT = 3;
  private static final int MAX_LOG_RETURN_SIZE = 10;
  private static final int SAMPLE_SIZE = 2;
  @Inject DataStoreService dataStoreService;
  @Inject WingsPersistence wingsPersistence;
  @Inject FeatureFlagService featureFlagService;

  public List<LogDataRecord> getLogRecordsToClassify(String accountId) {
    if (!featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, accountId)) {
      return null;
    }

    Query<LogMLAnalysisRecord> logQuery = wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority)
                                              .field("createdAt")
                                              .greaterThan(1514808000000l)
                                              .order("-createdAt");

    logQuery.or(logQuery.criteria("timesLabeled").lessThanOrEq(MAX_CASSIFICATION_COUNT),
        logQuery.criteria("timesLabeled").doesNotExist());

    List<LogMLAnalysisRecord> logAnalysisRecords = logQuery.asList(new FindOptions().limit(100));
    logAnalysisRecords.forEach(logAnalysisRecord -> logAnalysisRecord.decompressLogAnalysisRecord());

    List<LogDataRecord> logDataRecords = new ArrayList<>();

    List<LogDataRecord> returnList = new ArrayList<>();
    logAnalysisRecords.forEach(logAnalysisRecord -> {
      Optional
          .ofNullable(createDataRecords(
              logAnalysisRecord.getUuid(), logAnalysisRecord.getTimesLabeled(), logAnalysisRecord.getTest_clusters()))
          .ifPresent(logDataRecords::addAll);
      Optional
          .ofNullable(createDataRecords(logAnalysisRecord.getUuid(), logAnalysisRecord.getTimesLabeled(),
              logAnalysisRecord.getControl_clusters()))
          .ifPresent(logDataRecords::addAll);
      Optional
          .ofNullable(createDataRecords(logAnalysisRecord.getUuid(), logAnalysisRecord.getTimesLabeled(),
              logAnalysisRecord.getUnknown_clusters()))
          .ifPresent(logDataRecords::addAll);
      Optional
          .ofNullable(createDataRecords(
              logAnalysisRecord.getUuid(), logAnalysisRecord.getTimesLabeled(), logAnalysisRecord.getIgnore_clusters()))
          .ifPresent(logDataRecords::addAll);
    });
    if (logDataRecords.size() <= MAX_CASSIFICATION_COUNT) {
      returnList.addAll(logDataRecords);
    } else {
      while (returnList.size() < MAX_LOG_RETURN_SIZE) {
        Random randomizer = new Random();
        returnList.add(logDataRecords.get(randomizer.nextInt(logDataRecords.size())));
      }
    }
    return returnList;
  }

  private List<LogDataRecord> createDataRecords(
      String uuid, int timesLabelled, Map<String, Map<String, SplunkAnalysisCluster>> clusters) {
    if (isEmpty(clusters)) {
      return null;
    }
    List<LogDataRecord> returnList = new ArrayList<>();
    for (Map.Entry<String, Map<String, SplunkAnalysisCluster>> entry : clusters.entrySet()) {
      if (isNotEmpty(entry.getValue())) {
        for (SplunkAnalysisCluster cluster : entry.getValue().values()) {
          LogDataRecord record = new LogDataRecord();
          record.setLogMessage(cluster.getText());
          record.setUuid(uuid);
          record.setTimesLabeled(timesLabelled);
          returnList.add(record);
        }
      }
    }
    return returnList;
  }

  public void saveClassifiedLogRecord(LogDataRecord record, List<LogLabel> labels, String accountId, Object params) {
    if (!featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, accountId)) {
      return;
    }

    LabeledLogRecord classifiedRecord = JsonUtils.asObject(JsonUtils.asJson(params), LabeledLogRecord.class);
    record = classifiedRecord.getDataRecord();
    labels = classifiedRecord.getLabels();
    // see if there exists a LogClassifyRecord.
    if (!(dataStoreService instanceof GoogleDataStoreServiceImpl)) {
      logger.info("Google data store is not enabled. Returning.");
      return;
    }

    PageRequest<LabeledLogRecord> pageRequest = PageRequestBuilder.aPageRequest()
                                                    .withLimit(UNLIMITED)
                                                    .addFilter("dataRecordId", Operator.EQ, record.getUuid())
                                                    .build();

    final PageResponse<LabeledLogRecord> response = dataStoreService.list(LabeledLogRecord.class, pageRequest);
    if (response.getTotal() > 1) {
      logger.info("Got more than one labeled record with same ID: {}. Returning.", record.getUuid());
      return;
    }
    LabeledLogRecord labeledLogRecord;
    if (response.isEmpty()) {
      labeledLogRecord = LabeledLogRecord.builder()
                             .dataRecordId(record.getUuid())
                             .logMessage(record.getLogMessage())
                             .labels(labels)
                             .timesLabeled(1)
                             .build();
      if (isEmpty(labeledLogRecord.getUsers())) {
        labeledLogRecord.setUsers(new ArrayList<>());
      }
    } else {
      labeledLogRecord = response.getResponse().get(0);
      labels.addAll(labeledLogRecord.getLabels());
      labeledLogRecord.setLabels(labels);
    }
    List<String> userList = labeledLogRecord.getUsers();
    userList.add(UserThreadLocal.get().getPublicUser().getName());
    labeledLogRecord.setUsers(userList);

    // save the labelled record.
    dataStoreService.save(LabeledLogRecord.class, Arrays.asList(labeledLogRecord), false);

    // increment the timesLabeled count in logDataRecords

    Query<LogMLAnalysisRecord> query =
        wingsPersistence.createQuery(LogMLAnalysisRecord.class).filter("_id", record.getUuid());

    UpdateOperations<LogMLAnalysisRecord> updateOperations =
        wingsPersistence.createUpdateOperations(LogMLAnalysisRecord.class)
            .set("timesLabeled", record.getTimesLabeled() + 1);
    wingsPersistence.findAndModify(query, updateOperations, new FindAndModifyOptions());
  }

  public List<LogLabel> getLabels() {
    return Arrays.asList(LogLabel.values());
  }

  /**
   * This method will return a currently unclassified ignore feedback for this account/service combo.
   * @param accountId
   * @param serviceId
   * @return
   */
  public LogMLFeedbackRecord getIgnoreFeedbackToClassify(String accountId, String serviceId) {
    if (!featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, accountId)) {
      return null;
    }
    PageRequest<LogMLFeedbackRecord> feedbackRecordPageRequest =
        PageRequestBuilder.aPageRequest().addFilter("serviceId", Operator.EQ, serviceId).withLimit(UNLIMITED).build();
    List<LogMLFeedbackRecord> feedbackRecords =
        dataStoreService.list(LogMLFeedbackRecord.class, feedbackRecordPageRequest);
    for (LogMLFeedbackRecord record : feedbackRecords) {
      if (isEmpty(record.getSupervisedLabel())) {
        return record;
      }
    }
    return null;
  }

  /**
   * This method will return samples (2) of each label from the existing records for this account/service.
   * @param accountId
   * @param serviceId
   * @return
   */
  public Map<String, List<LogMLFeedbackRecord>> getLabeledSamplesForIgnoreFeedback(String accountId, String serviceId) {
    if (!featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, accountId)) {
      return null;
    }

    PageRequest<LogMLFeedbackRecord> feedbackRecordPageRequest =
        PageRequestBuilder.aPageRequest().addFilter("serviceId", Operator.EQ, serviceId).withLimit(UNLIMITED).build();

    Map<String, List<LogMLFeedbackRecord>> sampleRecords = new HashMap<>(), returnSamples = new HashMap<>();
    List<LogMLFeedbackRecord> feedbackRecords =
        dataStoreService.list(LogMLFeedbackRecord.class, feedbackRecordPageRequest);
    feedbackRecords.forEach(feedbackRecord -> {
      String label = feedbackRecord.getSupervisedLabel();
      if (isNotEmpty(label)) {
        if (!sampleRecords.containsKey(label)) {
          sampleRecords.put(label, new ArrayList<>());
        }
        sampleRecords.get(label).add(feedbackRecord);
      }
    });

    // randomize 2 per label.
    if (isNotEmpty(sampleRecords)) {
      sampleRecords.forEach((label, samples) -> {
        List<LogMLFeedbackRecord> samplesForLabel = new ArrayList<>();
        if (samples.size() <= 2) {
          samplesForLabel = samples;
        } else {
          Random randomizer = new Random();
          while (samplesForLabel.size() < SAMPLE_SIZE) {
            samplesForLabel.add(samples.get(randomizer.nextInt(samples.size())));
          }
        }
        returnSamples.put(label, samplesForLabel);
      });
    }

    return returnSamples;
  }

  /**
   * Saves the ignore feedback with a label
   * @return
   */
  public boolean saveLabeledIgnoreFeedback(String accountId, LogMLFeedbackRecord feedbackRecord, String label) {
    if (!featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, accountId)) {
      return false;
    }
    feedbackRecord.setSupervisedLabel(label);
    dataStoreService.save(LogMLFeedbackRecord.class, Arrays.asList(feedbackRecord), false);
    return true;
  }
}
