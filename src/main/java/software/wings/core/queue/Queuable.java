package software.wings.core.queue;

import org.mongodb.morphia.annotations.*;
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

  protected Queuable() {}
  public Queuable(Queuable other) {
    id = other.id;
    running = other.running;
    resetTimestamp = other.resetTimestamp;
    earliestGet = other.earliestGet;
    priority = other.priority;
    created = other.created;
  }

  public String getId() {
    return id;
  }

  public boolean isRunning() {
    return running;
  }

  public Date getResetTimestamp() {
    return resetTimestamp;
  }

  public Date getEarliestGet() {
    return earliestGet;
  }

  public double getPriority() {
    return priority;
  }

  public Date getCreated() {
    return created;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setRunning(boolean running) {
    this.running = running;
  }

  public void setResetTimestamp(Date resetTimestamp) {
    this.resetTimestamp = resetTimestamp;
  }

  public void setEarliestGet(Date earliestGet) {
    this.earliestGet = earliestGet;
  }

  public void setPriority(double priority) {
    this.priority = priority;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  @PrePersist
  public void onUpdate() {
    if (id == null) {
      id = UUIDGenerator.getUUID();
    }
  }
}
