package software.wings.beans.baseline;

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

/**
 * Created by rsingh on 2/16/18.
 */
@Entity(value = "workflowExecutionBaselines", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("workflowId"), @Field("envId"), @Field("serviceId")
  }, options = @IndexOptions(unique = true, name = "baselineUniqueIndex"))
})
@Data
@Builder
@EqualsAndHashCode(callSuper = false, exclude = {"workflowExecutionId"})
public class WorkflowExecutionBaseline extends Base {
  @NotEmpty @Indexed private String workflowId;
  @NotEmpty @Indexed private String envId;
  @NotEmpty @Indexed private String serviceId;
  @NotEmpty @Indexed private String workflowExecutionId;
  private String pipelineExecutionId;
}
