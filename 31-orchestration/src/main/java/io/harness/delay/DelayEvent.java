package io.harness.delay;

import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;

import java.util.Date;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
@Entity(value = "delayQueue", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class DelayEvent extends Queuable {
  private String resumeId;
  private Map<String, String> context;

  public DelayEvent(String resumeId, Date earliestGet, Map<String, String> context) {
    super(earliestGet);
    this.resumeId = resumeId;
    this.context = context;
  }
}
