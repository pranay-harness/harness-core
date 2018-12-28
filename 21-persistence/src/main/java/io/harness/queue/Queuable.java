package io.harness.queue;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.persistence.PersistentEntity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.utils.IndexType;

import java.util.Date;

@Indexes({
  @Index(options = @IndexOptions(name = "next"),
      fields =
      {
        @Field(value = "priority", type = IndexType.DESC)
        , @Field("created"), @Field("earliestGet"), @Field("running"), @Field("resetTimestamp"), @Field("version")
      })
  ,
      @Index(options = @IndexOptions(name = "count"), fields = { @Field("running") }),
})
public abstract class Queuable implements PersistentEntity {
  public static final String RUNNING_KEY = "running";
  public static final String RESET_TIMESTAMP_KEY = "resetTimestamp";
  public static final String EARLIEST_GET_KEY = "earliestGet";
  public static final String PRIORITY_KEY = "priority";
  public static final String CREATED_KEY = "created";
  public static final String VERSION_KEY = "version";

  @Id private String id;
  private boolean running;
  private Date resetTimestamp = new Date(Long.MAX_VALUE);
  private Date earliestGet = new Date();
  private double priority;
  private Date created = new Date();
  private int retries;
  private String version;

  /**
   * Instantiates a new queuable.
   */
  protected Queuable() {}

  protected Queuable(Date earliestGet) {
    this.earliestGet = earliestGet;
  }

  /**
   * Instantiates a new queuable.
   *
   * @param other Queuable to copy from.
   */
  public Queuable(Queuable other) {
    id = other.id;
    running = other.running;
    resetTimestamp = other.resetTimestamp;
    earliestGet = other.earliestGet;
    priority = other.priority;
    created = other.created;
    retries = other.retries;
    version = other.version;
  }

  /**
   * Gets id.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets id.
   *
   * @param id the id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Is running boolean.
   *
   * @return the boolean
   */
  public boolean isRunning() {
    return running;
  }

  /**
   * Sets running.
   *
   * @param running the running
   */
  public void setRunning(boolean running) {
    this.running = running;
  }

  /**
   * Gets reset timestamp.
   *
   * @return the reset timestamp
   */
  public Date getResetTimestamp() {
    return new Date(resetTimestamp.getTime());
  }

  /**
   * Sets reset timestamp.
   *
   * @param resetTimestamp the reset timestamp
   */
  public void setResetTimestamp(Date resetTimestamp) {
    this.resetTimestamp = new Date(resetTimestamp.getTime());
  }

  /**
   * Gets earliest get.
   *
   * @return the earliest get
   */
  public Date getEarliestGet() {
    return new Date(earliestGet.getTime());
  }

  /**
   * Sets earliest get.
   *
   * @param earliestGet the earliest get
   */
  public void setEarliestGet(Date earliestGet) {
    this.earliestGet = new Date(earliestGet.getTime());
  }

  /**
   * Gets priority.
   *
   * @return the priority
   */
  public double getPriority() {
    return priority;
  }

  /**
   * Sets priority.
   *
   * @param priority the priority
   */
  public void setPriority(double priority) {
    this.priority = priority;
  }

  /**
   * Gets created.
   *
   * @return the created
   */
  public Date getCreated() {
    return new Date(created.getTime());
  }

  /**
   * Sets created.
   *
   * @param created the created
   */
  public void setCreated(Date created) {
    this.created = new Date(created.getTime());
  }

  /**
   * Gets retries.
   *
   * @return the retries
   */
  public int getRetries() {
    return retries;
  }

  /**
   * Sets retries.
   *
   * @param retries the retries
   */
  public void setRetries(int retries) {
    this.retries = retries;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * pre save hook for mongo.
   */
  @PrePersist
  public void onUpdate() {
    if (id == null) {
      id = generateUuid();
    }
  }
}
