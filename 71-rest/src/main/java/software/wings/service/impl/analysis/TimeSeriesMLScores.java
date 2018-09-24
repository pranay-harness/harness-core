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
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;

/**
 * Created by sriram_parthasarathy on 10/17/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity(value = "timeSeriesMLScores", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("applicationId")
                           , @Field("workflowId"), @Field("stateType") },
    options = @IndexOptions(name = "ScoresUniqueIdx")))
@NoArgsConstructor
public class TimeSeriesMLScores extends Base {
  @NotEmpty @Indexed private StateType stateType;

  @NotEmpty @Indexed private String workflowId;

  @NotEmpty @Indexed private String workflowExecutionId;

  @NotEmpty @Indexed private String stateExecutionId;

  @NotEmpty @Indexed private int analysisMinute;

  private Map<String, TimeSeriesMLTxnScores> scoresMap;

  @Builder
  public TimeSeriesMLScores(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath, StateType stateType,
      String workflowId, String workflowExecutionId, String stateExecutionId, int analysisMinute,
      Map<String, TimeSeriesMLTxnScores> scoresMap) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.stateType = stateType;
    this.workflowId = workflowId;
    this.workflowExecutionId = workflowExecutionId;
    this.stateExecutionId = stateExecutionId;
    this.analysisMinute = analysisMinute;
    this.scoresMap = scoresMap;
  }
}
