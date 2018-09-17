package software.wings.beans;

import io.harness.data.validator.Trimmed;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Map;
import javax.validation.constraints.NotNull;

@Entity(value = "sweepingOutput", noClassnameStored = true)
@EqualsAndHashCode(callSuper = true)
@Indexes(@Index(fields = { @Field("name")
                           , @Field("workflowExecutionId") },
    options = @IndexOptions(name = "uniqueWorkflowExecution")))
public class SweepingOutput extends Base {
  public static final String PIPELINE_EXECUTION_ID_KEY = "pipelineExecutionId";
  public static final String WORKFLOW_EXECUTION_ID_KEY = "workflowExecutionId";

  String pipelineExecutionId;
  String workflowExecutionId;

  @NotNull @Trimmed private String name;
  @Getter Map<String, Object> variables;

  @Builder
  SweepingOutput(String appId, String pipelineExecutionId, String workflowExecutionId, String name,
      Map<String, Object> variables) {
    setAppId(appId);
    this.pipelineExecutionId = pipelineExecutionId;
    this.workflowExecutionId = workflowExecutionId;
    this.name = name;
    this.variables = variables;
  }
}
