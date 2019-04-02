package io.harness.beans;

import io.harness.data.validator.Trimmed;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import java.time.OffsetDateTime;
import java.util.Date;
import javax.validation.constraints.NotNull;

@Entity(value = "sweepingOutput", noClassnameStored = true)
@Value
@Builder
@Indexes({
  @Index(options = @IndexOptions(name = "uniquePipelineExecution", unique = true),
      fields = { @Field("appId")
                 , @Field("name"), @Field("pipelineExecutionId") })
  ,
      @Index(options = @IndexOptions(name = "uniqueWorkflowExecution", unique = true), fields = {
        @Field("appId"), @Field("name"), @Field("workflowExecutionId")
      }), @Index(options = @IndexOptions(name = "uniquePhaseExecution", unique = true), fields = {
        @Field("appId"), @Field("name"), @Field("phaseExecutionId")
      })
})
public class SweepingOutput implements PersistentEntity, UuidAccess {
  public static final String APP_ID_KEY = "appId";
  public static final String NAME_KEY = "name";
  public static final String PHASE_EXECUTION_ID_KEY = "phaseExecutionId";
  public static final String PIPELINE_EXECUTION_ID_KEY = "pipelineExecutionId";
  public static final String WORKFLOW_EXECUTION_ID_KEY = "workflowExecutionId";

  @Id private String uuid;
  private String appId;
  private String pipelineExecutionId;
  private String workflowExecutionId;
  private String phaseExecutionId;

  @NotNull @Trimmed private String name;
  @Getter private byte[] output;

  public enum Scope { PIPELINE, WORKFLOW, PHASE }

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());
}
