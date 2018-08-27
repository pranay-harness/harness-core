package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.service.impl.splunk.LogMLClusterScores;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 6/23/17.
 */
@Entity(value = "logAnalysisRecords", noClassnameStored = true)
@Indexes(@Index(fields =
    { @Field("applicationId")
      , @Field("stateExecutionId"), @Field("stateType"), @Field("logCollectionMinute") },
    options = @IndexOptions(unique = true, name = "logAnalysisUniqueIdx")))
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class LogMLAnalysisRecord extends Base {
  @NotEmpty @Indexed private String stateExecutionId;

  @NotEmpty @Indexed private StateType stateType;

  @NotEmpty @Indexed private int logCollectionMinute;

  private boolean isBaseLineCreated = true;
  private String baseLineExecutionId;

  private String query;
  private String analysisSummaryMessage;
  private double score;
  private LogMLClusterScores cluster_scores;
  private byte[] analysisDetailsCompressedJson;

  private @Transient List<List<SplunkAnalysisCluster>> unknown_events;
  private @Transient Map<String, List<SplunkAnalysisCluster>> test_events;
  private @Transient Map<String, List<SplunkAnalysisCluster>> control_events;
  private @Transient Map<String, Map<String, SplunkAnalysisCluster>> control_clusters;
  private @Transient Map<String, Map<String, SplunkAnalysisCluster>> unknown_clusters;
  private @Transient Map<String, Map<String, SplunkAnalysisCluster>> test_clusters;
  private @Transient Map<String, Map<String, SplunkAnalysisCluster>> ignore_clusters;

  @Builder
  private LogMLAnalysisRecord(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath,
      String stateExecutionId, StateType stateType, int logCollectionMinute, boolean isBaseLineCreated,
      String baseLineExecutionId, String query, String analysisSummaryMessage, double score,
      List<List<SplunkAnalysisCluster>> unknown_events, Map<String, List<SplunkAnalysisCluster>> test_events,
      Map<String, List<SplunkAnalysisCluster>> control_events,
      Map<String, Map<String, SplunkAnalysisCluster>> control_clusters,
      Map<String, Map<String, SplunkAnalysisCluster>> unknown_clusters,
      Map<String, Map<String, SplunkAnalysisCluster>> test_clusters,
      Map<String, Map<String, SplunkAnalysisCluster>> ignore_clusters, LogMLClusterScores cluster_scores,
      byte[] analysisDetailsCompressedJson) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.stateExecutionId = stateExecutionId;
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
  }
}
