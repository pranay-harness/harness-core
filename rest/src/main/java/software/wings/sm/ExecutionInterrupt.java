/**
 *
 */

package software.wings.sm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;

import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * The type Workflow execution event.
 *
 * @author Rishi
 */
@Entity(value = "executionInterrupts", noClassnameStored = true)
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
public class ExecutionInterrupt extends Base {
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

    public ExecutionInterruptBuilder withExecutionInterruptType(ExecutionInterruptType executionInterruptType) {
      this.executionInterruptType = executionInterruptType;
      return this;
    }

    public ExecutionInterruptBuilder withSeized(boolean seized) {
      this.seized = seized;
      return this;
    }

    public ExecutionInterruptBuilder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public ExecutionInterruptBuilder withExecutionUuid(String executionUuid) {
      this.executionUuid = executionUuid;
      return this;
    }

    public ExecutionInterruptBuilder withStateExecutionInstanceId(String stateExecutionInstanceId) {
      this.stateExecutionInstanceId = stateExecutionInstanceId;
      return this;
    }

    public ExecutionInterruptBuilder withProperties(Map<String, Object> properties) {
      this.properties = properties;
      return this;
    }

    public ExecutionInterruptBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public ExecutionInterruptBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public ExecutionInterruptBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public ExecutionInterruptBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public ExecutionInterruptBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public ExecutionInterruptBuilder withLastUpdatedAt(long lastUpdatedAt) {
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
