package io.harness.event.model;

import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public final class GenericEvent extends Queuable {
  private Event event;
}
