package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.data.validator.Trimmed;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import java.time.OffsetDateTime;
import java.util.Date;
import javax.validation.constraints.NotNull;

@Entity(value = "sweepingOutput", noClassnameStored = true)
@EqualsAndHashCode(callSuper = true)
@Indexes({
  @Index(options = @IndexOptions(name = "uniquePipelineExecution"),
      fields = { @Field("appId")
                 , @Field("name"), @Field("pipelineExecutionId") })
  ,
      @Index(options = @IndexOptions(name = "uniqueWorkflowExecution"), fields = {
        @Field("appId"), @Field("name"), @Field("workflowExecutionId")
      })
})
public class SweepingOutput extends Base {
  public static final String NAME_KEY = "name";
  public static final String PIPELINE_EXECUTION_ID_KEY = "pipelineExecutionId";
  public static final String WORKFLOW_EXECUTION_ID_KEY = "workflowExecutionId";

  String pipelineExecutionId;
  String workflowExecutionId;

  @NotNull @Trimmed private String name;
  @Getter private byte[] output;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());

  @Builder
  SweepingOutput(String appId, String pipelineExecutionId, String workflowExecutionId, String name, byte[] output) {
    setAppId(appId);
    this.pipelineExecutionId = pipelineExecutionId;
    this.workflowExecutionId = workflowExecutionId;
    this.name = name;
    this.output = output;
  }
}
