package io.harness.delay;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.queue.Queuable;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;

import java.util.Date;
import java.util.Map;

@OwnedBy(CDC)
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
