package software.wings.core.queue;

import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.PrePersist;
import software.wings.common.UUIDGenerator;

import java.util.Date;

/**
 * Created by peeyushaggarwal on 4/11/16.
 */
@Indexes({
  @Index(value = "getStuckIndex", fields = { @Field("running")
                                             , @Field("resetTimestamp") })
  , @Index(value = "countIndex", fields = { @Field("running") }), @Index(value = "getIndex", fields = {
    @Field("running"), @Field("priority"), @Field("created"), @Field("earliestGet")
  })
})
public abstract class Queuable {
  @Id private String id;
  @Indexed private boolean running = false;
  private Date resetTimestamp = new Date(Long.MAX_VALUE);
  private Date earliestGet = new Date();
  private double priority = 0.0;
  private Date created = new Date();
  private int retries = 0;

  protected Queuable() {}

  /**
   * @param other Queueable to copy from.
   */
  public Queuable(Queuable other) {
    id = other.id;
    running = other.running;
    resetTimestamp = other.resetTimestamp;
    earliestGet = other.earliestGet;
    priority = other.priority;
    created = other.created;
    retries = other.retries;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public boolean isRunning() {
    return running;
  }

  public void setRunning(boolean running) {
    this.running = running;
  }

  public Date getResetTimestamp() {
    return new Date(resetTimestamp.getTime());
  }

  public void setResetTimestamp(Date resetTimestamp) {
    this.resetTimestamp = new Date(resetTimestamp.getTime());
  }

  public Date getEarliestGet() {
    return new Date(earliestGet.getTime());
  }

  public void setEarliestGet(Date earliestGet) {
    this.earliestGet = new Date(earliestGet.getTime());
  }

  public double getPriority() {
    return priority;
  }

  public void setPriority(double priority) {
    this.priority = priority;
  }

  public Date getCreated() {
    return new Date(created.getTime());
  }

  public void setCreated(Date created) {
    this.created = new Date(created.getTime());
  }

  public int getRetries() {
    return retries;
  }

  public void setRetries(int retries) {
    this.retries = retries;
  }

  /**
   * pre save hook for mongo.
   */
  @PrePersist
  public void onUpdate() {
    if (id == null) {
      id = UUIDGenerator.getUuid();
    }
  }
}
