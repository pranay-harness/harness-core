package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.sm.StateType;

@Entity(value = "logMlFeedbackRecords", noClassnameStored = true)
@Data
@Builder
public class LogMLFeedbackRecord extends Base {
  @NotEmpty @Indexed private String applicationId;

  @NotEmpty @Indexed private String serviceId;

  @NotEmpty @Indexed private String workflowId;

  @NotEmpty @Indexed private String workflowExecutionId;

  @NotEmpty @Indexed private String stateExecutionId;

  @NotEmpty @Indexed private StateType stateType;

  @NotEmpty @Indexed private AnalysisServiceImpl.FeedbackType feedbackType;

  @NotEmpty private String logMessage;

  @NotEmpty private String logMD5Hash;
}
