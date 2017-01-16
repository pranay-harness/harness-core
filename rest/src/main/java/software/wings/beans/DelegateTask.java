package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.apache.commons.codec.binary.Base64;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import software.wings.beans.DelegateTask.Converter;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

/**
 * Created by peeyushaggarwal on 12/5/16.
 */
@Entity(value = "delegateTasks", noClassnameStored = true)
@Converters(Converter.class)
public class DelegateTask extends Base {
  private TaskType taskType;
  private Object[] parameters;
  private String tag;
  private String accountId;
  private String waitId;
  private String topicName;

  /**
   * Getter for property 'taskType'.
   *
   * @return Value for property 'taskType'.
   */
  public TaskType getTaskType() {
    return taskType;
  }

  /**
   * Setter for property 'taskType'.
   *
   * @param taskType Value to set for property 'taskType'.
   */
  public void setTaskType(TaskType taskType) {
    this.taskType = taskType;
  }

  /**
   * Getter for property 'parameters'.
   *
   * @return Value for property 'parameters'.
   */
  public Object[] getParameters() {
    return parameters;
  }

  /**
   * Setter for property 'parameters'.
   *
   * @param parameters Value to set for property 'parameters'.
   */
  public void setParameters(Object[] parameters) {
    this.parameters = parameters;
  }

  /**
   * Getter for property 'tag'.
   *
   * @return Value for property 'tag'.
   */
  public String getTag() {
    return tag;
  }

  /**
   * Setter for property 'tag'.
   *
   * @param tag Value to set for property 'tag'.
   */
  public void setTag(String tag) {
    this.tag = tag;
  }

  /**
   * Getter for property 'accountId'.
   *
   * @return Value for property 'accountId'.
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Setter for property 'accountId'.
   *
   * @param accountId Value to set for property 'accountId'.
   */
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /**
   * Getter for property 'waitId'.
   *
   * @return Value for property 'waitId'.
   */
  public String getWaitId() {
    return waitId;
  }

  /**
   * Setter for property 'waitId'.
   *
   * @param waitId Value to set for property 'waitId'.
   */
  public void setWaitId(String waitId) {
    this.waitId = waitId;
  }

  /**
   * Getter for property 'topicName'.
   *
   * @return Value for property 'topicName'.
   */
  public String getTopicName() {
    return topicName;
  }

  /**
   * Setter for property 'topicName'.
   *
   * @param topicName Value to set for property 'topicName'.
   */
  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(taskType, parameters, tag, accountId, waitId, topicName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final DelegateTask other = (DelegateTask) obj;
    return Objects.equals(this.taskType, other.taskType) && Objects.deepEquals(this.parameters, other.parameters)
        && Objects.equals(this.tag, other.tag) && Objects.equals(this.accountId, other.accountId)
        && Objects.equals(this.waitId, other.waitId) && Objects.equals(this.topicName, other.topicName);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("taskType", taskType)
        .add("parameters", parameters)
        .add("tag", tag)
        .add("accountId", accountId)
        .add("waitId", waitId)
        .add("topicName", topicName)
        .toString();
  }

  public static final class Builder {
    private TaskType taskType;
    private Object[] parameters;
    private String tag;
    private String accountId;
    private String waitId;
    private String topicName;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    public static Builder aDelegateTask() {
      return new Builder();
    }

    public Builder withTaskType(TaskType taskType) {
      this.taskType = taskType;
      return this;
    }

    public Builder withParameters(Object[] parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder withTag(String tag) {
      this.tag = tag;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withWaitId(String waitId) {
      this.waitId = waitId;
      return this;
    }

    public Builder withTopicName(String topicName) {
      this.topicName = topicName;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder but() {
      return aDelegateTask()
          .withTaskType(taskType)
          .withParameters(parameters)
          .withTag(tag)
          .withAccountId(accountId)
          .withWaitId(waitId)
          .withTopicName(topicName)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    public DelegateTask build() {
      DelegateTask delegateTask = new DelegateTask();
      delegateTask.setTaskType(taskType);
      delegateTask.setParameters(parameters);
      delegateTask.setTag(tag);
      delegateTask.setAccountId(accountId);
      delegateTask.setWaitId(waitId);
      delegateTask.setTopicName(topicName);
      delegateTask.setUuid(uuid);
      delegateTask.setAppId(appId);
      delegateTask.setCreatedBy(createdBy);
      delegateTask.setCreatedAt(createdAt);
      delegateTask.setLastUpdatedBy(lastUpdatedBy);
      delegateTask.setLastUpdatedAt(lastUpdatedAt);
      return delegateTask;
    }
  }

  public static class Context {
    private String accountId;
    private String appId;

    /**
     * Getter for property 'accountId'.
     *
     * @return Value for property 'accountId'.
     */
    public String getAccountId() {
      return accountId;
    }

    /**
     * Setter for property 'accountId'.
     *
     * @param accountId Value to set for property 'accountId'.
     */
    public void setAccountId(String accountId) {
      this.accountId = accountId;
    }

    /**
     * Getter for property 'appId'.
     *
     * @return Value for property 'appId'.
     */
    public String getAppId() {
      return appId;
    }

    /**
     * Setter for property 'appId'.
     *
     * @param appId Value to set for property 'appId'.
     */
    public void setAppId(String appId) {
      this.appId = appId;
    }

    public static final class Builder {
      private String accountId;
      private String appId;

      private Builder() {}

      public static Builder aContext() {
        return new Builder();
      }

      public Builder withAccountId(String accountId) {
        this.accountId = accountId;
        return this;
      }

      public Builder withAppId(String appId) {
        this.appId = appId;
        return this;
      }

      public Builder but() {
        return aContext().withAccountId(accountId).withAppId(appId);
      }

      public Context build() {
        Context context = new Context();
        context.setAccountId(accountId);
        context.setAppId(appId);
        return context;
      }
    }
  }

  public static class Converter extends TypeConverter {
    private static final ThreadLocal<Kryo> kryos = ThreadLocal.withInitial(() -> new Kryo());

    public Converter() {
      super(Object[].class);
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      Output output = new Output(byteArrayOutputStream);
      kryos.get().writeClassAndObject(output, value);
      output.flush();
      return Base64.encodeBase64String(byteArrayOutputStream.toByteArray());
    }

    @Override
    public Object decode(Class<?> targetClass, Object fromDBObject, MappedField optionalExtraInfo) {
      return kryos.get().readClassAndObject(new Input(Base64.decodeBase64((String) fromDBObject)));
    }
  }
}
