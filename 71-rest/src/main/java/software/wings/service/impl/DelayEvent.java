package software.wings.service.impl;

import io.harness.queue.Queuable;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;

import java.util.Date;
import java.util.Map;

@Entity(value = "delayQueue", noClassnameStored = true)
@Value
@EqualsAndHashCode(callSuper = false)
public class DelayEvent extends Queuable {
  private String resumeId;
  private Map<String, String> context;

  public DelayEvent(String resumeId, Date earliestGet, Map<String, String> context) {
    super(earliestGet);
    this.resumeId = resumeId;
    this.context = context;
  }
}
