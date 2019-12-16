package software.wings.service.impl.analysis;

import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.common.VerificationConstants.ML_RECORDS_TTL_MONTHS;

import com.google.protobuf.InvalidProtocolBufferException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.data.encoding.EncodingUtils;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;
import software.wings.beans.Base;
import software.wings.service.impl.splunk.FrequencyPattern;
import software.wings.service.impl.splunk.LogAnalysisResult;
import software.wings.service.impl.splunk.LogMLClusterScores;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.service.impl.splunk.SplunkAnalysisCluster.MessageFrequency;
import software.wings.service.impl.verification.generated.LogMLAnalysisRecordProto.LogAnalysisCluster;
import software.wings.service.impl.verification.generated.LogMLAnalysisRecordProto.LogAnalysisClusterList;
import software.wings.service.impl.verification.generated.LogMLAnalysisRecordProto.LogAnalysisClusterMap;
import software.wings.service.impl.verification.generated.LogMLAnalysisRecordProto.LogAnalysisMessageFrequency;
import software.wings.service.impl.verification.generated.LogMLAnalysisRecordProto.LogMLAnalysisRecordDetails;
import software.wings.sm.StateType;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 6/23/17.
 */
@Indexes({
  @Index(fields =
      { @Field("cvConfigId")
        , @Field("analysisStatus"), @Field(value = "logCollectionMinute", type = IndexType.DESC) },
      options = @IndexOptions(name = "cvConfigId_1_analysisStatus_1_logCollectionMinute_-1"))
  ,
      @Index(fields =
          {
            @Field("cvConfigId")
            , @Field("deprecated"), @Field(value = "logCollectionMinute", type = IndexType.DESC),
                @Field(value = "lastUpdatedAt", type = IndexType.DESC)
          },
          options = @IndexOptions(name = "cvConfigId_1_deprecated_1_logCollectionMinute_-1_lastUpdatedAt_-1")),
      @Index(fields = {
        @Field("stateExecutionId")
        , @Field(value = "logCollectionMinute", type = IndexType.DESC), @Field("analysisStatus"),
            @Field(value = "lastUpdatedAt", type = IndexType.DESC)
      }, options = @IndexOptions(name = "analysisSummaryIdx")), @Index(fields = {
        @Field("cvConfigId")
        , @Field(value = "logCollectionMinute", type = IndexType.DESC), @Field("deprecated"),
            @Field(value = "lastUpdatedAt", type = IndexType.DESC),
      }, options = @IndexOptions(name = "cvConfigLogCollectionMinLastUpdatedIdx")), @Index(fields = {
        @Field("cvConfigId")
        , @Field(value = "analysisStatus"), @Field(value = "logCollectionMinute", type = IndexType.DESC),
            @Field("deprecated"),
      }, options = @IndexOptions(name = "cvConfigLogCollectionMinAnalysisStatusDeprecatedIndx")), @Index(fields = {
        @Field("stateExecutionId"), @Field("analysisStatus")
      }, options = @IndexOptions(name = "stateExecStatusIdx")), @Index(fields = {
        @Field("stateExecutionId")
        , @Field("analysisStatus"), @Field(value = "logCollectionMinute", type = IndexType.DESC)
      }, options = @IndexOptions(name = "stateExecutionId_1_analysisStatus_1_logCollectionMinute_-1"))
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "LogMLAnalysisRecordKeys")
@Entity(value = "logAnalysisRecords", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class LogMLAnalysisRecord extends Base {
  @NotEmpty private String stateExecutionId;
  private String cvConfigId;
  @Indexed private String workflowExecutionId;

  @NotEmpty private StateType stateType;

  @NotEmpty private int logCollectionMinute;

  private boolean isBaseLineCreated = true;
  private String baseLineExecutionId;

  private String query;
  private String analysisSummaryMessage;
  private double score;
  private LogMLClusterScores cluster_scores;
  private byte[] analysisDetailsCompressedJson;

  private LogMLAnalysisStatus analysisStatus;

  private byte[] protoSerializedAnalyisDetails;

  private List<List<SplunkAnalysisCluster>> unknown_events;
  private Map<String, List<SplunkAnalysisCluster>> test_events;
  private Map<String, List<SplunkAnalysisCluster>> control_events;
  private Map<String, Map<String, SplunkAnalysisCluster>> control_clusters;
  private Map<String, Map<String, SplunkAnalysisCluster>> unknown_clusters;
  private Map<String, Map<String, SplunkAnalysisCluster>> test_clusters;
  private Map<String, Map<String, SplunkAnalysisCluster>> ignore_clusters;
  private Map<String, FrequencyPattern> frequency_patterns;
  private Map<String, LogAnalysisResult> log_analysis_result;
  private double overallScore = -1.0;
  private int timesLabeled;
  private boolean deprecated;

  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());

  @Builder
  private LogMLAnalysisRecord(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String stateExecutionId, String cvConfigId,
      String workflowExecutionId, StateType stateType, int logCollectionMinute, boolean isBaseLineCreated,
      String baseLineExecutionId, String query, String analysisSummaryMessage, double score,
      List<List<SplunkAnalysisCluster>> unknown_events, Map<String, List<SplunkAnalysisCluster>> test_events,
      Map<String, List<SplunkAnalysisCluster>> control_events,
      Map<String, Map<String, SplunkAnalysisCluster>> control_clusters,
      Map<String, Map<String, SplunkAnalysisCluster>> unknown_clusters,
      Map<String, Map<String, SplunkAnalysisCluster>> test_clusters,
      Map<String, Map<String, SplunkAnalysisCluster>> ignore_clusters, LogMLClusterScores cluster_scores,
      byte[] analysisDetailsCompressedJson) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.stateExecutionId = stateExecutionId;
    this.cvConfigId = cvConfigId;
    this.workflowExecutionId = workflowExecutionId;
    this.stateType = stateType;
    this.logCollectionMinute = logCollectionMinute;
    this.isBaseLineCreated = isBaseLineCreated;
    this.baseLineExecutionId = baseLineExecutionId;
    this.query = query;
    this.analysisSummaryMessage = analysisSummaryMessage;
    this.score = score;
    this.unknown_events = unknown_events;
    this.test_events = test_events;
    this.control_events = control_events;
    this.control_clusters = control_clusters;
    this.unknown_clusters = unknown_clusters;
    this.test_clusters = test_clusters;
    this.ignore_clusters = ignore_clusters;
    this.cluster_scores = cluster_scores;
    this.analysisDetailsCompressedJson = analysisDetailsCompressedJson;
    this.validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());
  }

  public void decompressLogAnalysisRecord() {
    if (isNotEmpty(protoSerializedAnalyisDetails)) {
      try {
        LogMLAnalysisRecordDetails analysisRecordDetails =
            LogMLAnalysisRecordDetails.parseFrom(EncodingUtils.deCompressBytes(protoSerializedAnalyisDetails));
        if (analysisRecordDetails.getUnknownEventsCount() > 0) {
          this.unknown_events = new ArrayList<>();
          analysisRecordDetails.getUnknownEventsList().forEach(logAnalysisClusterList -> {
            List<SplunkAnalysisCluster> splunkAnalysisClusters = new ArrayList<>();
            logAnalysisClusterList.getAnalysisClustersList().forEach(
                logAnalysisCluster -> splunkAnalysisClusters.add(convertToSplunkAnalysisCluster(logAnalysisCluster)));
            this.unknown_events.add(splunkAnalysisClusters);
          });
        }

        if (analysisRecordDetails.getTestEventsCount() > 0) {
          this.test_events = new HashMap<>();
          analysisRecordDetails.getTestEventsMap().forEach((key, logAnalysisClusterList) -> {
            List<SplunkAnalysisCluster> splunkAnalysisClusters = new ArrayList<>();
            logAnalysisClusterList.getAnalysisClustersList().forEach(
                logAnalysisCluster -> splunkAnalysisClusters.add(convertToSplunkAnalysisCluster(logAnalysisCluster)));
            this.test_events.put(key, splunkAnalysisClusters);
          });
        }

        if (analysisRecordDetails.getControlEventsCount() > 0) {
          this.control_events = new HashMap<>();
          analysisRecordDetails.getControlEventsMap().forEach((key, logAnalysisClusterList) -> {
            List<SplunkAnalysisCluster> splunkAnalysisClusters = new ArrayList<>();
            logAnalysisClusterList.getAnalysisClustersList().forEach(
                logAnalysisCluster -> splunkAnalysisClusters.add(convertToSplunkAnalysisCluster(logAnalysisCluster)));
            this.control_events.put(key, splunkAnalysisClusters);
          });
        }

        if (analysisRecordDetails.getControlClustersCount() > 0) {
          this.control_clusters = new HashMap<>();
          analysisRecordDetails.getControlClustersMap().forEach((key, logAnalysisClusterMap) -> {
            Map<String, SplunkAnalysisCluster> splunkAnalysisClusterMap = new HashMap<>();
            logAnalysisClusterMap.getAnalysisClustersMapMap().forEach(
                (s, logAnalysisCluster)
                    -> splunkAnalysisClusterMap.put(s, convertToSplunkAnalysisCluster(logAnalysisCluster)));
            this.control_clusters.put(key, splunkAnalysisClusterMap);
          });
        }

        if (analysisRecordDetails.getUnknownClustersCount() > 0) {
          this.unknown_clusters = new HashMap<>();
          analysisRecordDetails.getUnknownClustersMap().forEach((key, logAnalysisClusterMap) -> {
            Map<String, SplunkAnalysisCluster> splunkAnalysisClusterMap = new HashMap<>();
            logAnalysisClusterMap.getAnalysisClustersMapMap().forEach(
                (s, logAnalysisCluster)
                    -> splunkAnalysisClusterMap.put(s, convertToSplunkAnalysisCluster(logAnalysisCluster)));
            this.unknown_clusters.put(key, splunkAnalysisClusterMap);
          });
        }

        if (analysisRecordDetails.getTestClustersCount() > 0) {
          this.test_clusters = new HashMap<>();
          analysisRecordDetails.getTestClustersMap().forEach((key, logAnalysisClusterMap) -> {
            Map<String, SplunkAnalysisCluster> splunkAnalysisClusterMap = new HashMap<>();
            logAnalysisClusterMap.getAnalysisClustersMapMap().forEach(
                (s, logAnalysisCluster)
                    -> splunkAnalysisClusterMap.put(s, convertToSplunkAnalysisCluster(logAnalysisCluster)));
            this.test_clusters.put(key, splunkAnalysisClusterMap);
          });
        }

        if (analysisRecordDetails.getIgnoreClustersCount() > 0) {
          this.ignore_clusters = new HashMap<>();
          analysisRecordDetails.getIgnoreClustersMap().forEach((key, logAnalysisClusterMap) -> {
            Map<String, SplunkAnalysisCluster> splunkAnalysisClusterMap = new HashMap<>();
            logAnalysisClusterMap.getAnalysisClustersMapMap().forEach(
                (s, logAnalysisCluster)
                    -> splunkAnalysisClusterMap.put(s, convertToSplunkAnalysisCluster(logAnalysisCluster)));
            this.ignore_clusters.put(key, splunkAnalysisClusterMap);
          });
        }

        this.setProtoSerializedAnalyisDetails(null);
      } catch (InvalidProtocolBufferException e) {
        throw new WingsException(e);
      }
      return;
    }

    if (isNotEmpty(this.getAnalysisDetailsCompressedJson())) {
      try {
        String decompressedAnalysisDetailsJson = deCompressString(this.getAnalysisDetailsCompressedJson());
        LogMLAnalysisRecord logAnalysisDetails =
            JsonUtils.asObject(decompressedAnalysisDetailsJson, LogMLAnalysisRecord.class);
        this.setUnknown_events(logAnalysisDetails.getUnknown_events());
        this.setTest_events(logAnalysisDetails.getTest_events());
        this.setControl_events(logAnalysisDetails.getControl_events());
        this.setControl_clusters(logAnalysisDetails.getControl_clusters());
        this.setUnknown_clusters(logAnalysisDetails.getUnknown_clusters());
        this.setTest_clusters(logAnalysisDetails.getTest_clusters());
        this.setIgnore_clusters(logAnalysisDetails.getIgnore_clusters());
      } catch (IOException e) {
        throw new WingsException(e);
      }
    }
  }

  public void compressLogAnalysisRecord() {
    LogMLAnalysisRecordDetails.Builder detailsBuilder = LogMLAnalysisRecordDetails.newBuilder();
    if (isNotEmpty(unknown_events)) {
      unknown_events.forEach(splunkAnalysisClusters -> {
        final LogAnalysisClusterList.Builder clusterListBuilder = LogAnalysisClusterList.newBuilder();
        splunkAnalysisClusters.forEach(splunkAnalysisCluster
            -> clusterListBuilder.addAnalysisClusters(convertToLogAnalysisCluster(splunkAnalysisCluster)));
        detailsBuilder.addUnknownEvents(clusterListBuilder.build());
      });
    }

    if (isNotEmpty(test_events)) {
      test_events.forEach((key, splunkAnalysisClusters) -> {
        final LogAnalysisClusterList.Builder clusterListBuilder = LogAnalysisClusterList.newBuilder();
        splunkAnalysisClusters.forEach(splunkAnalysisCluster
            -> clusterListBuilder.addAnalysisClusters(convertToLogAnalysisCluster(splunkAnalysisCluster)));
        detailsBuilder.putTestEvents(key, clusterListBuilder.build());
      });
    }

    if (isNotEmpty(control_events)) {
      control_events.forEach((key, splunkAnalysisClusters) -> {
        final LogAnalysisClusterList.Builder clusterListBuilder = LogAnalysisClusterList.newBuilder();
        splunkAnalysisClusters.forEach(splunkAnalysisCluster
            -> clusterListBuilder.addAnalysisClusters(convertToLogAnalysisCluster(splunkAnalysisCluster)));
        detailsBuilder.putControlEvents(key, clusterListBuilder.build());
      });
    }

    if (isNotEmpty(control_clusters)) {
      control_clusters.forEach((key, analysisClusterMap) -> {
        LogAnalysisClusterMap.Builder clusterMapBuilder = LogAnalysisClusterMap.newBuilder();
        analysisClusterMap.forEach(
            (s, splunkAnalysisCluster)
                -> clusterMapBuilder.putAnalysisClustersMap(s, convertToLogAnalysisCluster(splunkAnalysisCluster)));
        detailsBuilder.putControlClusters(key, clusterMapBuilder.build());
      });
    }

    if (isNotEmpty(unknown_clusters)) {
      unknown_clusters.forEach((key, analysisClusterMap) -> {
        LogAnalysisClusterMap.Builder clusterMapBuilder = LogAnalysisClusterMap.newBuilder();
        analysisClusterMap.forEach(
            (s, splunkAnalysisCluster)
                -> clusterMapBuilder.putAnalysisClustersMap(s, convertToLogAnalysisCluster(splunkAnalysisCluster)));
        detailsBuilder.putUnknownClusters(key, clusterMapBuilder.build());
      });
    }

    if (isNotEmpty(test_clusters)) {
      test_clusters.forEach((key, analysisClusterMap) -> {
        LogAnalysisClusterMap.Builder clusterMapBuilder = LogAnalysisClusterMap.newBuilder();
        analysisClusterMap.forEach(
            (s, splunkAnalysisCluster)
                -> clusterMapBuilder.putAnalysisClustersMap(s, convertToLogAnalysisCluster(splunkAnalysisCluster)));
        detailsBuilder.putTestClusters(key, clusterMapBuilder.build());
      });
    }

    if (isNotEmpty(ignore_clusters)) {
      ignore_clusters.forEach((key, analysisClusterMap) -> {
        LogAnalysisClusterMap.Builder clusterMapBuilder = LogAnalysisClusterMap.newBuilder();
        analysisClusterMap.forEach(
            (s, splunkAnalysisCluster)
                -> clusterMapBuilder.putAnalysisClustersMap(s, convertToLogAnalysisCluster(splunkAnalysisCluster)));
        detailsBuilder.putIgnoreClusters(key, clusterMapBuilder.build());
      });
    }

    this.setProtoSerializedAnalyisDetails(EncodingUtils.compressBytes(detailsBuilder.build().toByteArray()));
    this.setUnknown_events(null);
    this.setTest_events(null);
    this.setControl_events(null);
    this.setControl_clusters(null);
    this.setUnknown_clusters(null);
    this.setTest_clusters(null);
    this.setIgnore_clusters(null);
  }

  private LogAnalysisCluster convertToLogAnalysisCluster(SplunkAnalysisCluster splunkAnalysisCluster) {
    List<LogAnalysisMessageFrequency> messageFrequenciesList = new ArrayList<>();
    if (isNotEmpty(splunkAnalysisCluster.getMessage_frequencies())) {
      splunkAnalysisCluster.getMessage_frequencies().forEach(messageFrequency
          -> messageFrequenciesList.add(
              LogAnalysisMessageFrequency.newBuilder()
                  .setCount(messageFrequency.getCount())
                  .setOldLabel(messageFrequency.getOldLabel() == null ? "" : messageFrequency.getOldLabel())
                  .setHost(messageFrequency.getHost() == null ? "" : messageFrequency.getHost())
                  .setTime(messageFrequency.getTime())
                  .build()));
    }
    LogAnalysisCluster.Builder cluster =
        LogAnalysisCluster.newBuilder()
            .setClusterLabel(splunkAnalysisCluster.getCluster_label())
            .setUnexpectedFreq(splunkAnalysisCluster.isUnexpected_freq())
            .setText(splunkAnalysisCluster.getText())
            .setX(splunkAnalysisCluster.getX())
            .setY(splunkAnalysisCluster.getY())
            .setFeedbackId(splunkAnalysisCluster.getFeedback_id() == null ? "" : splunkAnalysisCluster.getFeedback_id())
            .setAlertScore(splunkAnalysisCluster.getAlert_score())
            .setTestScore(splunkAnalysisCluster.getTest_score())
            .setControlScore(splunkAnalysisCluster.getControl_score())
            .setFreqScore(splunkAnalysisCluster.getFreq_score())
            .setControlLabel(splunkAnalysisCluster.getControl_label())
            .setRiskLevel(splunkAnalysisCluster.getRisk_level())
            .addAllTags(
                splunkAnalysisCluster.getTags() == null ? Collections.emptyList() : splunkAnalysisCluster.getTags())
            .addAllAnomalousCounts(splunkAnalysisCluster.getAnomalous_counts() == null
                    ? Collections.emptyList()
                    : splunkAnalysisCluster.getAnomalous_counts())
            .addAllDiffTags(splunkAnalysisCluster.getDiff_tags() == null ? Collections.emptyList()
                                                                         : splunkAnalysisCluster.getDiff_tags())
            .addAllMessageFrequencies(messageFrequenciesList);

    if (splunkAnalysisCluster.getPriority() != null) {
      cluster.setPriority(splunkAnalysisCluster.getPriority().name());
    }
    return cluster.build();
  }

  private SplunkAnalysisCluster convertToSplunkAnalysisCluster(LogAnalysisCluster logAnalysisCluster) {
    List<MessageFrequency> messageFrequencies = new ArrayList<>();
    if (isNotEmpty(logAnalysisCluster.getMessageFrequenciesList())) {
      logAnalysisCluster.getMessageFrequenciesList().forEach(logAnalysisMessageFrequency
          -> messageFrequencies.add(
              MessageFrequency.builder()
                  .count(logAnalysisMessageFrequency.getCount())
                  .oldLabel(logAnalysisMessageFrequency.getOldLabel().isEmpty()
                          ? null
                          : logAnalysisMessageFrequency.getOldLabel())
                  .host(logAnalysisMessageFrequency.getHost().isEmpty() ? null : logAnalysisMessageFrequency.getHost())
                  .time(logAnalysisMessageFrequency.getTime())
                  .build()));
    }
    SplunkAnalysisCluster splunkAnalysisCluster = new SplunkAnalysisCluster();
    splunkAnalysisCluster.setMessage_frequencies(messageFrequencies);
    splunkAnalysisCluster.setCluster_label(logAnalysisCluster.getClusterLabel());
    splunkAnalysisCluster.setTags(logAnalysisCluster.getTagsList());
    splunkAnalysisCluster.setAnomalous_counts(logAnalysisCluster.getAnomalousCountsList());
    splunkAnalysisCluster.setUnexpected_freq(logAnalysisCluster.getUnexpectedFreq());
    splunkAnalysisCluster.setText(logAnalysisCluster.getText());
    splunkAnalysisCluster.setX(logAnalysisCluster.getX());
    splunkAnalysisCluster.setY(logAnalysisCluster.getY());
    splunkAnalysisCluster.setFeedback_id(
        isEmpty(logAnalysisCluster.getFeedbackId()) ? null : logAnalysisCluster.getFeedbackId());
    splunkAnalysisCluster.setDiff_tags(logAnalysisCluster.getDiffTagsList());
    splunkAnalysisCluster.setAlert_score(logAnalysisCluster.getAlertScore());
    splunkAnalysisCluster.setTest_score(logAnalysisCluster.getTestScore());
    splunkAnalysisCluster.setControl_score(logAnalysisCluster.getControlScore());
    splunkAnalysisCluster.setFreq_score(logAnalysisCluster.getFreqScore());
    splunkAnalysisCluster.setControl_label(logAnalysisCluster.getControlLabel());
    splunkAnalysisCluster.setRisk_level(logAnalysisCluster.getRiskLevel());
    splunkAnalysisCluster.setPriority(
        isEmpty(logAnalysisCluster.getPriority()) ? null : FeedbackPriority.valueOf(logAnalysisCluster.getPriority()));

    return splunkAnalysisCluster;
  }
}
