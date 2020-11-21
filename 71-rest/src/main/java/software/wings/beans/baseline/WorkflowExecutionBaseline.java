package software.wings.beans.baseline;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;

import software.wings.beans.Base;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by rsingh on 2/16/18.
 */

@NgUniqueIndex(name = "baselineUniqueIndex", fields = { @Field("workflowId")
                                                        , @Field("envId"), @Field("serviceId") })
@Data
@Builder
@EqualsAndHashCode(callSuper = false, exclude = {"workflowExecutionId"})
@FieldNameConstants(innerTypeName = "WorkflowExecutionBaselineKeys")
@Entity(value = "workflowExecutionBaselines", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class WorkflowExecutionBaseline extends Base implements AccountAccess {
  public static final String WORKFLOW_ID_KEY = "workflowId";
  public static final String ENV_ID_KEY = "envId";
  public static final String SERVICE_ID_KEY = "serviceId";

  @NotEmpty private String workflowId;
  @NotEmpty private String envId;
  @NotEmpty private String serviceId;
  @NotEmpty @FdIndex private String workflowExecutionId;
  @FdIndex private String accountId;
  private String pipelineExecutionId;

  @UtilityClass
  public static class WorkflowExecutionBaselineKeys {
    // Temporary
    public static final String appId = "appId";
  }
}
