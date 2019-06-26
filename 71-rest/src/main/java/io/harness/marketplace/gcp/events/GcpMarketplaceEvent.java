package io.harness.marketplace.gcp.events;

import io.harness.marketplace.gcp.events.intfc.Event;
import io.harness.persistence.PersistentEntity;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Entity(value = "gcpMarketplaceEvents", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "GcpMarketplaceEventKeys")
public class GcpMarketplaceEvent implements PersistentEntity {
  @Id String messageId;
  Event event;
}
