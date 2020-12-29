package io.harness.cvng.core.services.impl;

import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_SERVICE_GUARD_WINDOW_SIZE;
import static io.harness.cvng.core.services.CVNextGenConstants.CV_ANALYSIS_WINDOW_MINUTES;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;

import static java.util.Comparator.comparingLong;

import io.harness.cvng.analysis.beans.TimeSeriesRecordDTO;
import io.harness.cvng.analysis.beans.TimeSeriesTestDataDTO;
import io.harness.cvng.analysis.beans.TimeSeriesTestDataDTO.MetricData;
import io.harness.cvng.analysis.entities.TimeSeriesRiskSummary;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.beans.HostRecordDTO;
import io.harness.cvng.beans.TimeSeriesDataCollectionRecord;
import io.harness.cvng.beans.TimeSeriesDataCollectionRecord.TimeSeriesDataRecordGroupValue;
import io.harness.cvng.beans.TimeSeriesDataCollectionRecord.TimeSeriesDataRecordMetricValue;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricCVConfig;
import io.harness.cvng.core.entities.TimeSeriesRecord;
import io.harness.cvng.core.entities.TimeSeriesRecord.TimeSeriesGroupValue;
import io.harness.cvng.core.entities.TimeSeriesRecord.TimeSeriesRecordKeys;
import io.harness.cvng.core.entities.TimeSeriesThreshold;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.HostRecordService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.UpdateOptions;
import org.mongodb.morphia.query.Query;

@Slf4j
public class TimeSeriesServiceImpl implements TimeSeriesService {
  @Inject private HPersistence hPersistence;
  @Inject private CVConfigService cvConfigService;
  @Inject private MetricPackService metricPackService;
  @Inject private HostRecordService hostRecordService;
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;

  @Override
  public boolean save(List<TimeSeriesDataCollectionRecord> dataRecords) {
    log.info("Saving {} data records", dataRecords.size());
    UpdateOptions options = new UpdateOptions();
    options.upsert(true);
    Map<TimeSeriesRecordBucketKey, TimeSeriesRecord> timeSeriesRecordMap = bucketTimeSeriesRecords(dataRecords);
    timeSeriesRecordMap.forEach((timeSeriesRecordBucketKey, timeSeriesRecord) -> {
      List<TimeSeriesMetricDefinition> metricTemplates =
          timeSeriesAnalysisService.getMetricTemplate(timeSeriesRecord.getVerificationTaskId());
      TimeSeriesMetricDefinition timeSeriesMetricDefinition =
          metricTemplates.stream()
              .filter(metricTemplate -> metricTemplate.getMetricName().equals(timeSeriesRecord.getMetricName()))
              .findFirst()
              .orElse(null);
      TimeSeriesMetricType metricType =
          timeSeriesMetricDefinition != null ? timeSeriesMetricDefinition.getMetricType() : null;
      Query<TimeSeriesRecord> query =
          hPersistence.createQuery(TimeSeriesRecord.class)
              .filter(
                  TimeSeriesRecordKeys.bucketStartTime, Instant.ofEpochMilli(timeSeriesRecordBucketKey.getTimestamp()))
              .filter(TimeSeriesRecordKeys.metricName, timeSeriesRecordBucketKey.getMetricName())
              .filter(TimeSeriesRecordKeys.verificationTaskId, timeSeriesRecord.getVerificationTaskId());
      if (timeSeriesRecord.getHost() != null) {
        query = query.filter(TimeSeriesRecordKeys.host, timeSeriesRecord.getHost());
      }
      hPersistence.getDatastore(TimeSeriesRecord.class)
          .update(query,
              hPersistence.createUpdateOperations(TimeSeriesRecord.class)
                  .setOnInsert(TimeSeriesRecordKeys.uuid, generateUuid())
                  .setOnInsert(TimeSeriesRecordKeys.createdAt, Instant.now().toEpochMilli())
                  .setOnInsert(TimeSeriesRecordKeys.metricType, metricType)
                  .set(TimeSeriesRecordKeys.accountId, timeSeriesRecord.getAccountId())
                  .addToSet(TimeSeriesRecordKeys.timeSeriesGroupValues,
                      Lists.newArrayList(timeSeriesRecord.getTimeSeriesGroupValues())),
              options);
    });

    saveHosts(dataRecords);
    return true;
  }

  @Value
  @Builder
  private static class TimeSeriesRecordBucketKey {
    String host;
    long timestamp;
    String metricName;
  }

  private void saveHosts(List<TimeSeriesDataCollectionRecord> dataRecords) {
    if (isNotEmpty(dataRecords)) {
      Preconditions.checkState(
          dataRecords.stream().map(dataRecord -> dataRecord.getVerificationTaskId()).distinct().count() == 1,
          "All the verificationIds should be same");
      String verificationTaskId = dataRecords.get(0).getVerificationTaskId();
      long minTimestamp = dataRecords.stream()
                              .map(dataRecord -> dataRecord.getTimeStamp())
                              .min(comparingLong(timestamp -> timestamp))
                              .get();
      long maxTimeStamp = dataRecords.stream()
                              .map(dataRecord -> dataRecord.getTimeStamp())
                              .max(comparingLong(timestamp -> timestamp))
                              .get();
      Set<String> hosts = dataRecords.stream()
                              .map(dataRecord -> dataRecord.getHost())
                              .filter(host -> host != null)
                              .collect(Collectors.toSet());
      if (isNotEmpty(hosts)) {
        HostRecordDTO hostRecordDTO = HostRecordDTO.builder()
                                          .verificationTaskId(verificationTaskId)
                                          .startTime(Instant.ofEpochMilli(minTimestamp))
                                          .endTime(Instant.ofEpochMilli(maxTimeStamp))
                                          .hosts(hosts)
                                          .build();
        hostRecordService.save(hostRecordDTO);
      }
    }
  }

  private Map<TimeSeriesRecordBucketKey, TimeSeriesRecord> bucketTimeSeriesRecords(
      List<TimeSeriesDataCollectionRecord> dataRecords) {
    Map<String, TimeSeriesMetricType> metricTypeMap = new HashMap<>();
    List<TimeSeriesMetricDefinition> metricDefinitions =
        timeSeriesAnalysisService.getMetricTemplate(dataRecords.get(0).getVerificationTaskId());
    metricDefinitions.forEach(timeSeriesMetricDefinition
        -> metricTypeMap.put(timeSeriesMetricDefinition.getMetricName(), timeSeriesMetricDefinition.getMetricType()));
    populatePercent(metricTypeMap, dataRecords);
    Map<TimeSeriesRecordBucketKey, TimeSeriesRecord> rv = new HashMap<>();
    dataRecords.forEach(dataRecord -> {
      long bucketBoundary = dataRecord.getTimeStamp()
          - Math.floorMod(dataRecord.getTimeStamp(), TimeUnit.MINUTES.toMillis(CV_ANALYSIS_WINDOW_MINUTES));
      dataRecord.getMetricValues().forEach(timeSeriesDataRecordMetricValue -> {
        String metricName = timeSeriesDataRecordMetricValue.getMetricName();
        TimeSeriesRecordBucketKey timeSeriesRecordBucketKey = TimeSeriesRecordBucketKey.builder()
                                                                  .host(dataRecord.getHost())
                                                                  .metricName(metricName)
                                                                  .timestamp(bucketBoundary)
                                                                  .build();
        if (!rv.containsKey(timeSeriesRecordBucketKey)) {
          rv.put(timeSeriesRecordBucketKey,
              TimeSeriesRecord.builder()
                  .accountId(dataRecord.getAccountId())
                  .verificationTaskId(dataRecord.getVerificationTaskId())
                  .host(dataRecord.getHost())
                  .accountId(dataRecord.getAccountId())
                  .bucketStartTime(Instant.ofEpochMilli(bucketBoundary))
                  .metricName(metricName)
                  .build());
        }

        timeSeriesDataRecordMetricValue.getTimeSeriesValues().forEach(timeSeriesDataRecordGroupValue
            -> rv.get(timeSeriesRecordBucketKey)
                   .getTimeSeriesGroupValues()
                   .add(TimeSeriesGroupValue.builder()
                            .groupName(timeSeriesDataRecordGroupValue.getGroupName())
                            .timeStamp(Instant.ofEpochMilli(dataRecord.getTimeStamp()))
                            .metricValue(timeSeriesDataRecordGroupValue.getValue())
                            .percentValue(TimeSeriesMetricType.ERROR.equals(metricTypeMap.get(metricName))
                                        && timeSeriesDataRecordGroupValue.getPercent() != null
                                    ? timeSeriesDataRecordGroupValue.getPercent()
                                    : null)
                            .build()));
      });
    });
    return rv;
  }

  private void populatePercent(
      Map<String, TimeSeriesMetricType> metricTypeMap, List<TimeSeriesDataCollectionRecord> dataRecords) {
    dataRecords.forEach(dataRecord -> {
      TimeSeriesDataRecordMetricValue throughput =
          dataRecord.getMetricValues()
              .stream()
              .filter(
                  metricValue -> TimeSeriesMetricType.THROUGHPUT.equals(metricTypeMap.get(metricValue.getMetricName())))
              .findFirst()
              .orElse(null);

      for (TimeSeriesDataRecordMetricValue metricValue : dataRecord.getMetricValues()) {
        if (!TimeSeriesMetricType.ERROR.equals(metricTypeMap.get(metricValue.getMetricName()))) {
          continue;
        }

        // if no throughput is configured then percent value is same as value
        if (throughput == null) {
          metricValue.getTimeSeriesValues().forEach(
              errorMetricValue -> errorMetricValue.setPercent(errorMetricValue.getValue()));
          return;
        }

        for (TimeSeriesDataRecordGroupValue throughputValue : throughput.getTimeSeriesValues()) {
          if (throughputValue.getValue() <= 0.0) {
            continue;
          }

          for (TimeSeriesDataRecordGroupValue errorMetricValue : metricValue.getTimeSeriesValues()) {
            if (!errorMetricValue.getGroupName().equals(throughputValue.getGroupName())) {
              continue;
            }

            errorMetricValue.setPercent((errorMetricValue.getValue() * 100) / throughputValue.getValue());
          }
        }
      }
    });
  }

  @Override
  public boolean updateRiskScores(String verificationTaskId, TimeSeriesRiskSummary riskSummary) {
    Set<String> metricNames = riskSummary.getTransactionMetricRiskList()
                                  .stream()
                                  .map(TimeSeriesRiskSummary.TransactionMetricRisk::getMetricName)
                                  .collect(Collectors.toSet());
    List<TimeSeriesRecord> records =
        hPersistence.createQuery(TimeSeriesRecord.class, excludeAuthority)
            .filter(TimeSeriesRecordKeys.verificationTaskId, riskSummary.getVerificationTaskId())
            .field(TimeSeriesRecordKeys.bucketStartTime)
            .greaterThanOrEq(
                riskSummary.getAnalysisEndTime().minus(TIMESERIES_SERVICE_GUARD_WINDOW_SIZE, ChronoUnit.MINUTES))
            .field(TimeSeriesRecordKeys.bucketStartTime)
            .lessThan(riskSummary.getAnalysisEndTime())
            .field(TimeSeriesRecordKeys.metricName)
            .in(metricNames)
            .asList();

    Map<String, List<TimeSeriesRecord>> metricNameRecordMap =
        records.stream().collect(Collectors.groupingBy(TimeSeriesRecord::getMetricName));

    riskSummary.getTransactionMetricRiskList().forEach(metricRisk -> {
      List<TimeSeriesRecord> timeSeriesRecords = metricNameRecordMap.get(metricRisk.getMetricName());
      if (isNotEmpty(timeSeriesRecords)) {
        timeSeriesRecords.forEach(record -> {
          String groupName = metricRisk.getTransactionName();
          record.getTimeSeriesGroupValues().forEach(groupValue -> {
            if (groupName.equals(groupValue.getGroupName())) {
              groupValue.setRiskScore(Math.max(metricRisk.getMetricRisk(), groupValue.getRiskScore()));
            }
          });
        });
      }
    });
    log.info("Updating the risk in {} timeseries records", records.size());
    hPersistence.save(records);

    return false;
  }

  @Override
  public List<TimeSeriesMetricDefinition> getTimeSeriesMetricDefinitions(String cvConfigId) {
    CVConfig cvConfig = cvConfigService.get(cvConfigId);
    Preconditions.checkNotNull(cvConfig, "could not find datasource with id ", cvConfigId);

    MetricCVConfig metricCVConfig = (MetricCVConfig) cvConfig;

    List<TimeSeriesMetricDefinition> timeSeriesMetricDefinitions = new ArrayList<>();
    // add project level thresholds
    List<TimeSeriesThreshold> metricPackThresholds = metricPackService.getMetricPackThresholds(
        metricCVConfig.getAccountId(), metricCVConfig.getOrgIdentifier(), metricCVConfig.getProjectIdentifier(),
        metricCVConfig.getMetricPack().getIdentifier(), metricCVConfig.getType());
    metricPackThresholds.forEach(timeSeriesThreshold
        -> timeSeriesMetricDefinitions.add(TimeSeriesMetricDefinition.builder()
                                               .metricName(timeSeriesThreshold.getMetricName())
                                               .metricType(timeSeriesThreshold.getMetricType())
                                               .metricGroupName(timeSeriesThreshold.getMetricGroupName())
                                               .actionType(timeSeriesThreshold.getAction())
                                               .comparisonType(timeSeriesThreshold.getCriteria().getType())
                                               .action(timeSeriesThreshold.getCriteria().getAction())
                                               .occurrenceCount(timeSeriesThreshold.getCriteria().getOccurrenceCount())
                                               .thresholdType(timeSeriesThreshold.getCriteria().getThresholdType())
                                               .value(timeSeriesThreshold.getCriteria().getValue())
                                               .build()));

    // add data source level thresholds
    metricCVConfig.getMetricPack().getMetrics().forEach(metricDefinition -> {
      if (isNotEmpty(metricDefinition.getThresholds())) {
        metricDefinition.getThresholds().forEach(timeSeriesThreshold
            -> timeSeriesMetricDefinitions.add(
                TimeSeriesMetricDefinition.builder()
                    .metricName(metricDefinition.getName())
                    .metricType(metricDefinition.getType())
                    .metricGroupName(timeSeriesThreshold.getMetricGroupName())
                    .actionType(timeSeriesThreshold.getAction())
                    .comparisonType(timeSeriesThreshold.getCriteria().getType())
                    .action(timeSeriesThreshold.getCriteria().getAction())
                    .occurrenceCount(timeSeriesThreshold.getCriteria().getOccurrenceCount())
                    .thresholdType(timeSeriesThreshold.getCriteria().getThresholdType())
                    .value(timeSeriesThreshold.getCriteria().getValue())
                    .build()));
      }
    });
    return timeSeriesMetricDefinitions;
  }

  @Override
  public TimeSeriesTestDataDTO getMetricGroupDataForRange(
      String cvConfigId, Instant startTime, Instant endTime, String metricName, List<String> groupNames) {
    Preconditions.checkNotNull(cvConfigId, "cvConfigId is null in getTimeseriesDataForRange");

    TimeSeriesTestDataDTO timeseriesData = getTxnMetricDataForRange(cvConfigId, startTime, endTime, metricName, null);

    Map<String, Map<String, List<MetricData>>> metricNameGroupNameValMap = new HashMap<>();
    if (timeseriesData != null) {
      timeseriesData.getMetricGroupValues().forEach((groupName, metricValueMap) -> {
        if (!metricNameGroupNameValMap.containsKey(metricName)) {
          metricNameGroupNameValMap.put(metricName, new HashMap<>());
        }

        if (isNotEmpty(groupNames) && groupNames.contains(groupName)) {
          List<MetricData> values = metricValueMap.get(metricName);
          if (!metricNameGroupNameValMap.containsKey(metricName)) {
            metricNameGroupNameValMap.put(metricName, new HashMap<>());
          }

          metricNameGroupNameValMap.get(metricName).put(groupName, values);
        } else if (isEmpty(groupNames)) {
          // we need to add for all transactions without filtering
          metricNameGroupNameValMap.get(metricName).put(groupName, metricValueMap.get(metricName));
        }
      });

      return TimeSeriesTestDataDTO.builder()
          .cvConfigId(cvConfigId)
          .metricGroupValues(metricNameGroupNameValMap)
          .build();
    }
    return null;
  }

  // TODO: create a overridden method without metric and txnName
  @Override
  public TimeSeriesTestDataDTO getTxnMetricDataForRange(
      String verificationTaskId, Instant startTime, Instant endTime, String metricName, String txnName) {
    List<TimeSeriesRecord> records = getTimeSeriesRecords(verificationTaskId, startTime, endTime, metricName);

    Map<String, List<TimeSeriesGroupValue>> metricValueList = new HashMap<>();
    records.forEach(record -> {
      if (!metricValueList.containsKey(record.getMetricName())) {
        metricValueList.put(record.getMetricName(), new ArrayList<>());
      }

      List<TimeSeriesRecord.TimeSeriesGroupValue> valueList = metricValueList.get(record.getMetricName());
      List<TimeSeriesRecord.TimeSeriesGroupValue> curValueList = new ArrayList<>();
      // if txnName filter is present, filter by that name
      if (isNotEmpty(txnName)) {
        record.getTimeSeriesGroupValues().forEach(timeSeriesGroupValue -> {
          if (timeSeriesGroupValue.getGroupName().equals(txnName)) {
            curValueList.add(timeSeriesGroupValue);
          }
        });
      } else {
        curValueList.addAll(record.getTimeSeriesGroupValues());
      }

      // filter for those timestamps that fall within the start and endTime
      curValueList.forEach(groupValue -> {
        boolean timestampInWindow =
            !(groupValue.getTimeStamp().isBefore(startTime) || groupValue.getTimeStamp().isAfter(endTime));
        if (timestampInWindow) {
          valueList.add(groupValue);
        }
      });

      metricValueList.put(record.getMetricName(), valueList);
    });

    return getSortedListOfTimeSeriesRecords(verificationTaskId, metricValueList);
  }
  // TODO: use accountId
  private List<TimeSeriesRecord> getTimeSeriesRecords(String verificationTaskId, Instant startTime, Instant endTime) {
    return getTimeSeriesRecords(verificationTaskId, startTime, endTime, null);
  }

  private List<TimeSeriesRecord> getTimeSeriesRecords(
      String verificationTaskId, Instant startTime, Instant endTime, String metricName) {
    startTime = DateTimeUtils.roundDownToMinBoundary(startTime, (int) CV_ANALYSIS_WINDOW_MINUTES);
    Instant queryStartTime = startTime.truncatedTo(ChronoUnit.SECONDS);
    Instant queryEndTime = endTime.truncatedTo(ChronoUnit.SECONDS);
    Query<TimeSeriesRecord> timeSeriesRecordsQuery =
        hPersistence.createQuery(TimeSeriesRecord.class, excludeAuthority)
            .filter(TimeSeriesRecordKeys.verificationTaskId, verificationTaskId)
            .field(TimeSeriesRecordKeys.bucketStartTime)
            .greaterThanOrEq(queryStartTime)
            .field(TimeSeriesRecordKeys.bucketStartTime)
            .lessThan(queryEndTime);
    if (isNotEmpty(metricName)) {
      timeSeriesRecordsQuery = timeSeriesRecordsQuery.filter(TimeSeriesRecordKeys.metricName, metricName);
    }
    // TODO: filter values that are outside of given time range.
    return timeSeriesRecordsQuery.asList();
  }

  @Override
  public List<TimeSeriesRecordDTO> getTimeSeriesRecordDTOs(
      String verificationTaskId, Instant startTime, Instant endTime) {
    List<TimeSeriesRecord> timeSeriesRecords = getTimeSeriesRecords(verificationTaskId, startTime, endTime);
    List<TimeSeriesRecordDTO> timeSeriesRecordDTOS = new ArrayList<>();
    timeSeriesRecords.forEach(timeSeriesRecord -> {
      for (TimeSeriesRecord.TimeSeriesGroupValue record : timeSeriesRecord.getTimeSeriesGroupValues()) {
        if (record.getTimeStamp().compareTo(startTime) >= 0 && record.getTimeStamp().compareTo(endTime) < 0) {
          TimeSeriesRecordDTO timeSeriesRecordDTO =
              TimeSeriesRecordDTO.builder()
                  .groupName(record.getGroupName())
                  .host(timeSeriesRecord.getHost())
                  .metricName(timeSeriesRecord.getMetricName())
                  .epochMinute(TimeUnit.MILLISECONDS.toMinutes(record.getTimeStamp().toEpochMilli()))
                  .verificationTaskId(timeSeriesRecord.getVerificationTaskId())
                  .metricValue(record.getMetricValue())
                  .build();
          timeSeriesRecordDTOS.add(timeSeriesRecordDTO);
        }
      }
    });
    return timeSeriesRecordDTOS;
  }

  private TimeSeriesTestDataDTO getSortedListOfTimeSeriesRecords(
      String cvConfigId, Map<String, List<TimeSeriesRecord.TimeSeriesGroupValue>> unsortedTimeseries) {
    if (isNotEmpty(unsortedTimeseries)) {
      Map<String, Map<String, List<TimeSeriesRecord.TimeSeriesGroupValue>>> txnMetricMap = new HashMap<>();

      // first build the txn -> metric -> TimeSeriesGroupValue object
      unsortedTimeseries.forEach((metricName, txnValList) -> {
        txnValList.forEach(txnValue -> {
          String txnName = txnValue.getGroupName();
          if (!txnMetricMap.containsKey(txnName)) {
            txnMetricMap.put(txnName, new HashMap<>());
          }
          if (!txnMetricMap.get(txnName).containsKey(metricName)) {
            txnMetricMap.get(txnName).put(metricName, new ArrayList<>());
          }

          txnMetricMap.get(txnName).get(metricName).add(txnValue);
        });
      });

      // next sort the list under each txn->metric
      Map<String, Map<String, List<Double>>> txnMetricValueMap = new HashMap<>();
      Map<String, Map<String, List<MetricData>>> metricGroupValueMap = new HashMap<>();
      for (String txnName : txnMetricMap.keySet()) {
        Map<String, List<TimeSeriesRecord.TimeSeriesGroupValue>> metricValueMap = txnMetricMap.get(txnName);
        txnMetricValueMap.put(txnName, new HashMap<>());
        metricGroupValueMap.put(txnName, new HashMap<>());
        for (String metricName : metricValueMap.keySet()) {
          List<TimeSeriesRecord.TimeSeriesGroupValue> valueList = metricValueMap.get(metricName);
          Collections.sort(valueList);
          txnMetricValueMap.get(txnName).put(metricName, new ArrayList<>());
          metricGroupValueMap.get(txnName).put(metricName, new ArrayList<>());
          valueList.forEach(value -> { txnMetricValueMap.get(txnName).get(metricName).add(value.getMetricValue()); });
          valueList.forEach(value -> {
            metricGroupValueMap.get(txnName)
                .get(metricName)
                .add(MetricData.builder()
                         .value(value.getMetricValue())
                         .timestamp(value.getTimeStamp().toEpochMilli())
                         .build());
          });
        }
      }

      return TimeSeriesTestDataDTO.builder()
          .cvConfigId(cvConfigId)
          .transactionMetricValues(txnMetricValueMap)
          .metricGroupValues(metricGroupValueMap)
          .build();
    }
    return null;
  }

  @Override
  public List<TimeSeriesRecord> getTimeSeriesRecordsForConfigs(
      List<String> verificationTaskIds, Instant startTime, Instant endTime, boolean anomalousOnly) {
    Query<TimeSeriesRecord> timeSeriesRecordQuery = hPersistence.createQuery(TimeSeriesRecord.class, excludeAuthority)
                                                        .field(TimeSeriesRecordKeys.verificationTaskId)
                                                        .in(verificationTaskIds)
                                                        .field(TimeSeriesRecordKeys.bucketStartTime)
                                                        .greaterThanOrEq(startTime)
                                                        .field(TimeSeriesRecordKeys.bucketStartTime)
                                                        .lessThan(endTime);

    if (anomalousOnly) {
      timeSeriesRecordQuery = timeSeriesRecordQuery
                                  .field(TimeSeriesRecordKeys.timeSeriesGroupValues + "."
                                      + TimeSeriesGroupValue.TimeSeriesValueKeys.riskScore)
                                  .greaterThan(0);
    }
    return timeSeriesRecordQuery.asList();
  }
}
