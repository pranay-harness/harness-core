package io.harness.event.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity(value = "genericEvent", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class GenericEvent extends Queuable {
  private Event event;
}
