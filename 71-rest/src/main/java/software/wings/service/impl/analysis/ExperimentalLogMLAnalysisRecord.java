package software.wings.service.impl.analysis;

import static software.wings.common.Constants.ML_RECORDS_TTL_MONTHS;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.service.impl.splunk.LogMLClusterScores;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.sm.StateType;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Entity(value = "experimentalLogAnalysisRecords", noClassnameStored = true)
@Indexes(@Index(fields =
    {
      @Field("applicationId")
      , @Field("stateExecutionId"), @Field("stateType"), @Field("experiment_name"), @Field("logCollectionMinute")
    },
    options = @IndexOptions(unique = true, name = "explogAnalysisUniqueIdx")))
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperimentalLogMLAnalysisRecord extends Base {
  @NotEmpty @Indexed private String stateExecutionId;

  @NotEmpty @Indexed private StateType stateType;
  @NotEmpty @Indexed private String experiment_name;

  @NotEmpty @Indexed private int logCollectionMinute;

  private boolean isBaseLineCreated = true;

  private String query;
  private String analysisSummaryMessage;
  private double score;
  private List<List<SplunkAnalysisCluster>> unknown_events;
  private Map<String, List<SplunkAnalysisCluster>> test_events;
  private Map<String, List<SplunkAnalysisCluster>> control_events;
  private Map<String, Map<String, SplunkAnalysisCluster>> control_clusters;
  private Map<String, Map<String, SplunkAnalysisCluster>> unknown_clusters;
  private Map<String, Map<String, SplunkAnalysisCluster>> test_clusters;
  private Map<String, Map<String, SplunkAnalysisCluster>> ignore_clusters;
  private LogMLClusterScores cluster_scores;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());
}
