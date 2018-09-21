package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.data.validator.Trimmed;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
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
  @Index(options = @IndexOptions(name = "uniquePipelineExecution", unique = true),
      fields = { @Field("appId")
                 , @Field("name"), @Field("pipelineExecutionId") })
  ,
      @Index(options = @IndexOptions(name = "uniqueWorkflowExecution", unique = true),
          fields = { @Field("appId")
                     , @Field("name"), @Field("workflowExecutionId") }),
      @Index(options = @IndexOptions(
                 name = "uniquePhaseExecution" /*, enable this after the migration is executed unique = true */),
          fields = { @Field("appId")
                     , @Field("name"), @Field("phaseExecutionId") })
})
@Value
public class SweepingOutput extends Base {
  public static final String NAME_KEY = "name";
  public static final String PIPELINE_EXECUTION_ID_KEY = "pipelineExecutionId";
  public static final String WORKFLOW_EXECUTION_ID_KEY = "workflowExecutionId";
  public static final String PHASE_EXECUTION_ID_KEY = "phaseExecutionId";

  String pipelineExecutionId;
  String workflowExecutionId;
  String phaseExecutionId;

  @NotNull @Trimmed private String name;
  @Getter private byte[] output;

  public enum Scope { PIPELINE, WORKFLOW, PHASE }

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());

  @Builder
  SweepingOutput(String appId, String pipelineExecutionId, String workflowExecutionId, String phaseExecutionId,
      String name, byte[] output) {
    setAppId(appId);
    this.pipelineExecutionId = pipelineExecutionId;
    this.workflowExecutionId = workflowExecutionId;
    this.phaseExecutionId = phaseExecutionId;
    this.name = name;
    this.output = output;
  }
}
