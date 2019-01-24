package software.wings.service.impl.analysis;

import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.GoogleDataStoreServiceImpl;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.analysis.LogLabelingService;
import software.wings.utils.JsonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Singleton
public class LogLabelingServiceImpl implements LogLabelingService {
  private static final Logger logger = LoggerFactory.getLogger(LogLabelingServiceImpl.class);
  private static final int MAX_CASSIFICATION_COUNT = 3;
  private static final int MAX_LOG_RETURN_SIZE = 10;
  @Inject DataStoreService dataStoreService;
  @Inject WingsPersistence wingsPersistence;
  @Inject FeatureFlagService featureFlagService;

  public List<LogDataRecord> getLogRecordsToClassify(String accountId) {
    if (!featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, accountId)) {
      return null;
    }

    Query<LogDataRecord> logQuery = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                                        .field("createdAt")
                                        .greaterThan(1514808000000l)
                                        .field("clusterLevel")
                                        .notIn(Arrays.asList("H0", "H1", "H2", "HF"))
                                        .project("uuid", true)
                                        .project("logMessage", true)
                                        .project("timesLabeled", true)
                                        .order("-createdAt");

    logQuery.or(logQuery.criteria("timesLabeled").lessThanOrEq(MAX_CASSIFICATION_COUNT),
        logQuery.criteria("timesLabeled").doesNotExist());

    List<LogDataRecord> logDataRecords = logQuery.asList(new FindOptions().limit(1000));
    List<LogDataRecord> returnList = new ArrayList<>();
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
    } else {
      labeledLogRecord = response.getResponse().get(0);
      labels.addAll(labeledLogRecord.getLabels());
      labeledLogRecord.setLabels(labels);
    }

    // save the labelled record.
    dataStoreService.save(LabeledLogRecord.class, Arrays.asList(labeledLogRecord));

    // increment the timesLabeled count in logDataRecords

    Query<LogDataRecord> query = wingsPersistence.createQuery(LogDataRecord.class).filter("_id", record.getUuid());

    UpdateOperations<LogDataRecord> updateOperations =
        wingsPersistence.createUpdateOperations(LogDataRecord.class).inc("timesLabeled");
    wingsPersistence.findAndModify(query, updateOperations, new FindAndModifyOptions());
  }

  public List<LogLabel> getLabels() {
    return Arrays.asList(LogLabel.values());
  }
}
