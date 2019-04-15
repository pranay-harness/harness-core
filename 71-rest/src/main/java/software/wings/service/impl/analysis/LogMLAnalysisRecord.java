package software.wings.service.impl.analysis;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.common.Constants.ML_RECORDS_TTL_MONTHS;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
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
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.service.impl.splunk.LogMLClusterScores;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.sm.StateType;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 6/23/17.
 */
@Entity(value = "logAnalysisRecords", noClassnameStored = true)
@Indexes(@Index(fields =
    {
      @Field("applicationId")
      , @Field("stateExecutionId"), @Field("cvConfigId"), @Field("stateType"), @Field("logCollectionMinute")
    },
    options = @IndexOptions(unique = true, name = "logAnalysisUniqueIdx")))
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "LogMLAnalysisRecordKeys")
public class LogMLAnalysisRecord extends Base {
  @NotEmpty @Indexed private String stateExecutionId;
  @Indexed private String cvConfigId;

  @NotEmpty private StateType stateType;

  @NotEmpty private int logCollectionMinute;

  private boolean isBaseLineCreated = true;
  private String baseLineExecutionId;

  private String query;
  private String analysisSummaryMessage;
  private double score;
  private LogMLClusterScores cluster_scores;
  private byte[] analysisDetailsCompressedJson;

  private List<List<SplunkAnalysisCluster>> unknown_events;
  private Map<String, List<SplunkAnalysisCluster>> test_events;
  private Map<String, List<SplunkAnalysisCluster>> control_events;
  private Map<String, Map<String, SplunkAnalysisCluster>> control_clusters;
  private Map<String, Map<String, SplunkAnalysisCluster>> unknown_clusters;
  private Map<String, Map<String, SplunkAnalysisCluster>> test_clusters;
  private Map<String, Map<String, SplunkAnalysisCluster>> ignore_clusters;
  private double overallScore = -1.0;
  private int timesLabeled;
  private boolean deprecated;

  @Transient
  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());

  @Builder
  private LogMLAnalysisRecord(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String stateExecutionId, String cvConfigId,
      StateType stateType, int logCollectionMinute, boolean isBaseLineCreated, String baseLineExecutionId, String query,
      String analysisSummaryMessage, double score, List<List<SplunkAnalysisCluster>> unknown_events,
      Map<String, List<SplunkAnalysisCluster>> test_events, Map<String, List<SplunkAnalysisCluster>> control_events,
      Map<String, Map<String, SplunkAnalysisCluster>> control_clusters,
      Map<String, Map<String, SplunkAnalysisCluster>> unknown_clusters,
      Map<String, Map<String, SplunkAnalysisCluster>> test_clusters,
      Map<String, Map<String, SplunkAnalysisCluster>> ignore_clusters, LogMLClusterScores cluster_scores,
      byte[] analysisDetailsCompressedJson) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.stateExecutionId = stateExecutionId;
    this.cvConfigId = cvConfigId;
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
    LogMLAnalysisRecord logAnalysisDetails = LogMLAnalysisRecord.builder()
                                                 .unknown_events(this.getUnknown_events())
                                                 .test_events(this.getTest_events())
                                                 .control_events(this.getControl_events())
                                                 .control_clusters(this.getControl_clusters())
                                                 .unknown_clusters(this.getUnknown_clusters())
                                                 .test_clusters(this.getTest_clusters())
                                                 .ignore_clusters(this.getIgnore_clusters())
                                                 .build();

    try {
      this.setAnalysisDetailsCompressedJson(compressString(JsonUtils.asJson(logAnalysisDetails)));
    } catch (IOException e) {
      throw new WingsException("failed to compress analysis details", e);
    }

    this.setUnknown_events(null);
    this.setTest_events(null);
    this.setControl_events(null);
    this.setControl_clusters(null);
    this.setUnknown_clusters(null);
    this.setTest_clusters(null);
    this.setIgnore_clusters(null);
  }
}
