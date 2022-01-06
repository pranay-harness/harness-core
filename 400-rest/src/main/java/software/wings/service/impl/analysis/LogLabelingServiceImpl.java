/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import static java.util.stream.Collectors.toMap;

import io.harness.beans.FeatureName;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.SearchFilter.Operator;
import io.harness.ff.FeatureFlagService;
import io.harness.time.Timestamp;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.CVFeedbackRecord.CVFeedbackRecordKeys;
import software.wings.service.impl.analysis.LabeledLogRecord.LabeledLogRecordKeys;
import software.wings.service.impl.analysis.LogDataRecord.LogDataRecordKeys;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.analysis.LogLabelingService;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;

@Singleton
@Slf4j
public class LogLabelingServiceImpl implements LogLabelingService {
  private static final SecureRandom random = new SecureRandom();

  private static final int MAX_CASSIFICATION_COUNT = 3;
  private static final int MAX_LOG_RETURN_SIZE = 10;
  private static final int SAMPLE_SIZE = 2;
  @Inject DataStoreService dataStoreService;
  @Inject WingsPersistence wingsPersistence;
  @Inject FeatureFlagService featureFlagService;
  @Inject AccountService accountService;

  @Override
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
    logAnalysisRecords.forEach(LogMLAnalysisRecord::decompressLogAnalysisRecord);

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
        returnList.add(logDataRecords.get(random.nextInt(logDataRecords.size())));
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

  @Override
  public void saveClassifiedLogRecord(LogDataRecord record, List<LogLabel> labels, String accountId, Object params) {}

  @Override
  public List<LogLabel> getLabels() {
    return Arrays.asList(LogLabel.values());
  }

  /**
   * This method will return a currently unclassified ignore feedback for this account/service combo.
   *
   * @param accountId
   * @param serviceId
   * @return
   */
  @Override
  public List<CVFeedbackRecord> getCVFeedbackToClassify(String accountId, String serviceId) {
    if (!featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, accountId)) {
      return null;
    }
    PageRequest<CVFeedbackRecord> feedbackRecordPageRequest =
        PageRequestBuilder.aPageRequest()
            .addFilter(CVFeedbackRecordKeys.serviceId, Operator.EQ, serviceId)
            .withLimit(UNLIMITED)
            .build();
    return getUnlabeledFeedback(feedbackRecordPageRequest, 5);
  }

  private List<CVFeedbackRecord> getUnlabeledFeedback(
      PageRequest<CVFeedbackRecord> feedbackRecordPageRequest, int count) {
    List<CVFeedbackRecord> feedbackRecords = dataStoreService.list(CVFeedbackRecord.class, feedbackRecordPageRequest);
    List<CVFeedbackRecord> returnList =
        feedbackRecords.stream()
            .filter(record -> isEmpty(record.getSupervisedLabel()) && !record.isDuplicate())
            .collect(Collectors.toList());

    int toIndex = Integer.min(returnList.size(), count);
    return returnList.subList(0, toIndex);
  }

  /**
   * This method will return a currently unclassified cv feedback for any account.
   *
   * @param accountId
   * @return
   */
  @Override
  public List<CVFeedbackRecord> getCVFeedbackToClassify(String accountId) {
    if (!featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, accountId)) {
      return null;
    }
    PageRequest<CVFeedbackRecord> feedbackRecordPageRequest =
        PageRequestBuilder.aPageRequest().withLimit(UNLIMITED).build();
    return getUnlabeledFeedback(feedbackRecordPageRequest, 5);
  }

  /**
   * This method will return samples (2) of each label from the existing records for this account/service.
   *
   * @param accountId
   * @param serviceId
   * @return
   */
  @Override
  public Map<String, List<CVFeedbackRecord>> getLabeledSamplesForIgnoreFeedback(
      String accountId, String serviceId, String envId) {
    if (!featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, accountId)) {
      return null;
    }

    PageRequest<CVFeedbackRecord> feedbackRecordPageRequest =
        PageRequestBuilder.aPageRequest()
            .addFilter(CVFeedbackRecordKeys.serviceId, Operator.EQ, serviceId)
            .addFilter(CVFeedbackRecordKeys.envId, Operator.EQ, envId)
            .withLimit(UNLIMITED)
            .build();

    return getSupervisedLabelSamplesForFeedback(feedbackRecordPageRequest);
  }

  private Map<String, List<CVFeedbackRecord>> getSupervisedLabelSamplesForFeedback(
      PageRequest<CVFeedbackRecord> feedbackRecordPageRequest) {
    Map<String, List<CVFeedbackRecord>> sampleRecords = new HashMap<>(), returnSamples = new HashMap<>();
    List<CVFeedbackRecord> feedbackRecords = dataStoreService.list(CVFeedbackRecord.class, feedbackRecordPageRequest);
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
        List<CVFeedbackRecord> samplesForLabel = new ArrayList<>();
        if (samples.size() <= 2) {
          samplesForLabel = samples;
        } else {
          while (samplesForLabel.size() < SAMPLE_SIZE) {
            samplesForLabel.add(samples.get(random.nextInt(samples.size())));
          }
        }
        returnSamples.put(label, samplesForLabel);
      });
    }

    return returnSamples;
  }

  /**
   * This method will return samples (2) of each label from the existing records for any account
   *
   * @param accountId
   * @return
   */
  @Override
  public Map<String, List<CVFeedbackRecord>> getLabeledSamplesForIgnoreFeedback(String accountId) {
    if (!featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, accountId)) {
      return null;
    }

    PageRequest<CVFeedbackRecord> feedbackRecordPageRequest =
        PageRequestBuilder.aPageRequest().withLimit(UNLIMITED).build();

    return getSupervisedLabelSamplesForFeedback(feedbackRecordPageRequest);
  }

  /**
   * Saves the cv feedback with a label
   *
   * @return
   */
  @Override
  public boolean saveLabeledIgnoreFeedback(String accountId, CVFeedbackRecord feedbackRecord, String label) {
    if (!featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, accountId)) {
      return false;
    }
    CVFeedbackRecord recordFromDB = dataStoreService.getEntity(CVFeedbackRecord.class, feedbackRecord.getUuid());
    recordFromDB.setSupervisedLabel(label);
    dataStoreService.save(CVFeedbackRecord.class, Arrays.asList(recordFromDB), false);
    return true;
  }

  /**
   * Saves the cv feedback with a label
   *
   * @return
   */
  @Override
  public boolean saveLabeledIgnoreFeedback(String accountId, Map<String, List<CVFeedbackRecord>> feedbackRecordMap) {
    if (!featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, accountId)) {
      return false;
    }
    if (isEmpty(feedbackRecordMap)) {
      return true;
    }
    List<CVFeedbackRecord> recordsToSave = new ArrayList<>();
    feedbackRecordMap.forEach((label, recordList) -> {
      recordList.forEach(record -> record.setSupervisedLabel(label));
      recordsToSave.addAll(recordList);
    });

    dataStoreService.save(CVFeedbackRecord.class, recordsToSave, false);
    return true;
  }

  @Override
  public Map<Pair<String, String>, Integer> getAccountsWithFeedback() {
    PageRequest<CVFeedbackRecord> feedbackRecordPageRequest =
        PageRequestBuilder.aPageRequest().withLimit(UNLIMITED).build();
    List<CVFeedbackRecord> records = dataStoreService.list(CVFeedbackRecord.class, feedbackRecordPageRequest);

    Map<Pair<String, String>, Integer> accountCount = new HashMap<>();

    records.forEach(cvFeedbackRecord -> {
      Account account = wingsPersistence.get(Account.class, cvFeedbackRecord.getAccountId());
      Pair<String, String> accountNamePair = Pair.of(account.getAccountName(), cvFeedbackRecord.getAccountId());
      if (!accountCount.containsKey(accountNamePair)) {
        accountCount.put(accountNamePair, 0);
      }
      accountCount.put(accountNamePair, accountCount.get(accountNamePair) + 1);
    });

    // sort by value
    return accountCount.entrySet()
        .stream()
        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
  }

  @Override
  public Map<Pair<String, String>, Integer> getServicesWithFeedbackForAccount(String accountId) {
    PageRequest<CVFeedbackRecord> feedbackRecordPageRequest =
        PageRequestBuilder.aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter(CVFeedbackRecordKeys.accountId, Operator.EQ, accountId)
            .build();
    List<CVFeedbackRecord> records = dataStoreService.list(CVFeedbackRecord.class, feedbackRecordPageRequest);

    Map<Pair<String, String>, Integer> serviceCount = new HashMap<>();

    records.forEach(cvFeedbackRecord -> {
      String serviceId = cvFeedbackRecord.getServiceId();
      Service service = wingsPersistence.get(Service.class, serviceId);
      String appName = wingsPersistence.get(Application.class, service.getAppId()).getName();

      String nameToDisplay = appName + "-" + service.getName();
      Pair<String, String> serviceNamePair = Pair.of(nameToDisplay, serviceId);
      if (!serviceCount.containsKey(serviceNamePair)) {
        serviceCount.put(serviceNamePair, 0);
      }
      serviceCount.put(serviceNamePair, serviceCount.get(serviceNamePair) + 1);
    });

    // sort by value
    return serviceCount.entrySet()
        .stream()
        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
  }

  @Override
  public Map<String, List<String>> getSampleLabeledRecords(String serviceId, String envId) {
    PageRequest<LabeledLogRecord> logRecordPageRequest =
        PageRequestBuilder.aPageRequest()
            .addFilter(LabeledLogRecordKeys.serviceId, Operator.EQ, serviceId)
            .withLimit(UNLIMITED)
            .build();

    List<LabeledLogRecord> labeledLogRecordList = dataStoreService.list(LabeledLogRecord.class, logRecordPageRequest);
    Map<String, List<String>> labelLogTextMap = new HashMap<>();
    if (isNotEmpty(labeledLogRecordList)) {
      labeledLogRecordList.forEach(record -> {
        List<String> feedbacksToFetch = new ArrayList<>(), l2IdsToFetch = new ArrayList<>();
        List<String> feedbackList =
            isNotEmpty(record.getFeedbackIds()) ? Lists.newArrayList(record.getFeedbackIds()) : new ArrayList<>();
        while (feedbacksToFetch.size() < feedbackList.size() && feedbacksToFetch.size() < 20) {
          feedbacksToFetch.add(feedbackList.get(random.nextInt(feedbackList.size())));
        }

        List<String> l2List = isNotEmpty(record.getLogDataRecordIds())
            ? Lists.newArrayList(record.getLogDataRecordIds())
            : new ArrayList<>();
        while (l2IdsToFetch.size() < l2List.size() && l2IdsToFetch.size() < 20) {
          l2IdsToFetch.add(l2List.get(random.nextInt(l2List.size())));
        }

        List<String> logTexts = new ArrayList<>();

        feedbacksToFetch.forEach(feedbackId -> {
          logTexts.add(dataStoreService.getEntity(CVFeedbackRecord.class, feedbackId).getLogMessage());
        });

        l2IdsToFetch.forEach(dataRecordId -> {
          logTexts.add(dataStoreService.getEntity(LogDataRecord.class, dataRecordId).getLogMessage());
        });

        labelLogTextMap.put(record.getLabel(), logTexts);
      });
    }
    return labelLogTextMap;
  }

  @Override
  public List<LogDataRecord> getL2RecordsToClassify(String serviceId, String envId) {
    PageRequest<LogDataRecord> logDataRecordPageRequest =
        PageRequestBuilder.aPageRequest()
            .addFilter(LogDataRecordKeys.serviceId, Operator.EQ, serviceId)
            .addFilter(LogDataRecordKeys.timeStamp, Operator.GE,
                Timestamp.currentMinuteBoundary() - TimeUnit.DAYS.toMillis(30))
            .build();

    List<LogDataRecord> logs = dataStoreService.list(LogDataRecord.class, logDataRecordPageRequest);
    if (isNotEmpty(logs)) {
      logs = logs.stream().filter(logObject -> logObject.getTimesLabeled() == 0).collect(Collectors.toList());
      List<LogDataRecord> logsToReturn = new ArrayList<>();
      while (logsToReturn.size() < 5) {
        logsToReturn.add(logs.get(random.nextInt(logs.size())));
      }
      return logsToReturn;
    }
    return null;
  }

  @Override
  public boolean saveLabeledL2AndFeedback(List<LabeledLogRecord> labeledLogRecords) {
    if (isNotEmpty(labeledLogRecords)) {
      labeledLogRecords.forEach(labeledLogRecord -> {
        String label = labeledLogRecord.getLabel();

        // check to see if this is available in GDS already
        PageRequest<LabeledLogRecord> pageRequest =
            PageRequestBuilder.aPageRequest()
                .addFilter(LabeledLogRecordKeys.label, Operator.EQ, label)
                .addFilter(LabeledLogRecordKeys.serviceId, Operator.EQ, labeledLogRecord.getServiceId())
                .build();
        List<LabeledLogRecord> recordList = dataStoreService.list(LabeledLogRecord.class, pageRequest);
        if (isEmpty(recordList)) {
          recordList = new ArrayList<>();
          recordList.add(labeledLogRecord);
        } else {
          // there will be only one record with this serviceId and label.
          if (recordList.get(0).getFeedbackIds() == null) {
            recordList.get(0).setFeedbackIds(new HashSet<>());
          }
          if (recordList.get(0).getLogDataRecordIds() == null) {
            recordList.get(0).setLogDataRecordIds(new HashSet<>());
          }
          recordList.get(0).getFeedbackIds().addAll(labeledLogRecord.getFeedbackIds());
          recordList.get(0).getLogDataRecordIds().addAll(labeledLogRecord.getLogDataRecordIds());
        }
        dataStoreService.save(LabeledLogRecord.class, recordList, false);

        // update the timesLabeled field in L2 records
        if (labeledLogRecord.getLogDataRecordIds() != null) {
          List<LogDataRecord> logDataRecordsToSave = new ArrayList<>();
          labeledLogRecord.getLogDataRecordIds().forEach(id -> {
            LogDataRecord record = dataStoreService.getEntity(LogDataRecord.class, id);
            record.setTimesLabeled(record.getTimesLabeled() + 1);
            record.setValidUntil(null);
            logDataRecordsToSave.add(record);
          });
          dataStoreService.save(LogDataRecord.class, logDataRecordsToSave, false);
        }

        if (labeledLogRecord.getFeedbackIds() != null) {
          List<CVFeedbackRecord> recordsToSave = new ArrayList<>();
          labeledLogRecord.getFeedbackIds().forEach(feedbackId -> {
            CVFeedbackRecord feedbackRecord = dataStoreService.getEntity(CVFeedbackRecord.class, feedbackId);
            feedbackRecord.setSupervisedLabel(label);
            recordsToSave.add(feedbackRecord);
          });
          dataStoreService.save(CVFeedbackRecord.class, recordsToSave, false);
        }
      });
    }
    return true;
  }
}
