package software.wings.service.impl.analysis;

import lombok.Builder;
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
import software.wings.sm.StateType;

@Entity(value = "logMlFeedbackRecords", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("applicationId"), @Field("stateExecutionId"), @Field("clusterType"), @Field("clusterLabel")
  }, options = @IndexOptions(unique = true, name = "logFeedbackUniqueIdx"))
})
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class LogMLFeedbackRecord extends Base {
  @NotEmpty @Indexed private String applicationId;

  @NotEmpty @Indexed private String serviceId;

  @NotEmpty @Indexed private String workflowId;

  @NotEmpty @Indexed private String workflowExecutionId;

  @NotEmpty @Indexed private String stateExecutionId;

  @NotEmpty @Indexed private StateType stateType;

  @NotEmpty @Indexed private int clusterLabel;

  @NotEmpty @Indexed private AnalysisServiceImpl.CLUSTER_TYPE clusterType;

  @NotEmpty @Indexed private AnalysisServiceImpl.LogMLFeedbackType logMLFeedbackType;

  @NotEmpty private String logMessage;

  @NotEmpty private String logMD5Hash;

  private String comment;
}
