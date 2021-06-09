package software.wings.service.impl.analysis;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.FdIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;

import software.wings.beans.Base;
import software.wings.sm.StateType;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by sriram_parthasarathy on 10/17/17.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "timeSeriesMLScores", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "TimeSeriesMLScoresKeys")
@StoreIn(DbAliases.ALL)
public class TimeSeriesMLScores extends Base implements AccountAccess {
  @NotEmpty private StateType stateType;

  @NotEmpty private String workflowId;

  @NotEmpty private String workflowExecutionId;

  @NotEmpty @FdIndex private String stateExecutionId;

  @NotEmpty private int analysisMinute;

  @FdIndex private String accountId;

  private Map<String, TimeSeriesMLTxnScores> scoresMap;

  @Builder
  public TimeSeriesMLScores(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, StateType stateType, String workflowId,
      String workflowExecutionId, String stateExecutionId, int analysisMinute,
      Map<String, TimeSeriesMLTxnScores> scoresMap, String accountId) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.stateType = stateType;
    this.workflowId = workflowId;
    this.workflowExecutionId = workflowExecutionId;
    this.stateExecutionId = stateExecutionId;
    this.analysisMinute = analysisMinute;
    this.accountId = accountId;
    this.scoresMap = scoresMap;
  }
}
