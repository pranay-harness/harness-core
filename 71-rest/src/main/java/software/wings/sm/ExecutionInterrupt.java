package software.wings.sm;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.entityinterface.ApplicationAccess;

import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * The type Workflow execution event.
 */
@Data
@Entity(value = "executionInterrupts", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "ExecutionInterruptKeys")
public class ExecutionInterrupt implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware,
                                           UpdatedByAware, ApplicationAccess {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @Indexed @NotNull @SchemaIgnore protected String appId;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @Indexed private long createdAt;

  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;

  @NotNull private ExecutionInterruptType executionInterruptType;

  // If true, means this interruption is no longer in effect
  private boolean seized;
  private String envId;
  @NotNull @Indexed private String executionUuid;
  @Indexed private String stateExecutionInstanceId;

  private Map<String, Object> properties;

  public boolean isSeized() {
    return seized;
  }

  public void setSeized(boolean seized) {
    this.seized = seized;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public String getExecutionUuid() {
    return executionUuid;
  }

  public void setExecutionUuid(String executionUuid) {
    this.executionUuid = executionUuid;
  }

  public ExecutionInterruptType getExecutionInterruptType() {
    return executionInterruptType;
  }

  public void setExecutionInterruptType(ExecutionInterruptType executionInterruptType) {
    this.executionInterruptType = executionInterruptType;
  }

  public String getStateExecutionInstanceId() {
    return stateExecutionInstanceId;
  }

  public void setStateExecutionInstanceId(String stateExecutionInstanceId) {
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ExecutionInterrupt)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    ExecutionInterrupt that = (ExecutionInterrupt) o;
    return seized == that.seized && executionInterruptType == that.executionInterruptType
        && Objects.equals(envId, that.envId) && Objects.equals(executionUuid, that.executionUuid)
        && Objects.equals(stateExecutionInstanceId, that.stateExecutionInstanceId)
        && Objects.equals(properties, that.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(), executionInterruptType, seized, envId, executionUuid, stateExecutionInstanceId, properties);
  }

  public static final class ExecutionInterruptBuilder {
    protected String appId;
    private ExecutionInterruptType executionInterruptType;
    private boolean seized;
    private String envId;
    private String executionUuid;
    private String stateExecutionInstanceId;
    private Map<String, Object> properties;
    private String uuid;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private ExecutionInterruptBuilder() {}

    public static ExecutionInterruptBuilder anExecutionInterrupt() {
      return new ExecutionInterruptBuilder();
    }

    public ExecutionInterruptBuilder executionInterruptType(ExecutionInterruptType executionInterruptType) {
      this.executionInterruptType = executionInterruptType;
      return this;
    }

    public ExecutionInterruptBuilder seized(boolean seized) {
      this.seized = seized;
      return this;
    }

    public ExecutionInterruptBuilder envId(String envId) {
      this.envId = envId;
      return this;
    }

    public ExecutionInterruptBuilder executionUuid(String executionUuid) {
      this.executionUuid = executionUuid;
      return this;
    }

    public ExecutionInterruptBuilder stateExecutionInstanceId(String stateExecutionInstanceId) {
      this.stateExecutionInstanceId = stateExecutionInstanceId;
      return this;
    }

    public ExecutionInterruptBuilder properties(Map<String, Object> properties) {
      this.properties = properties;
      return this;
    }

    public ExecutionInterruptBuilder uuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public ExecutionInterruptBuilder appId(String appId) {
      this.appId = appId;
      return this;
    }

    public ExecutionInterruptBuilder createdBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public ExecutionInterruptBuilder createdAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public ExecutionInterruptBuilder lastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public ExecutionInterruptBuilder lastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public ExecutionInterrupt build() {
      ExecutionInterrupt executionInterrupt = new ExecutionInterrupt();
      executionInterrupt.setExecutionInterruptType(executionInterruptType);
      executionInterrupt.setSeized(seized);
      executionInterrupt.setEnvId(envId);
      executionInterrupt.setExecutionUuid(executionUuid);
      executionInterrupt.setStateExecutionInstanceId(stateExecutionInstanceId);
      executionInterrupt.setProperties(properties);
      executionInterrupt.setUuid(uuid);
      executionInterrupt.setAppId(appId);
      executionInterrupt.setCreatedBy(createdBy);
      executionInterrupt.setCreatedAt(createdAt);
      executionInterrupt.setLastUpdatedBy(lastUpdatedBy);
      executionInterrupt.setLastUpdatedAt(lastUpdatedAt);
      return executionInterrupt;
    }
  }
}
