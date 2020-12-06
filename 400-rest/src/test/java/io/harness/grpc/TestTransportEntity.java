package io.harness.grpc;

import io.harness.persistence.PersistentEntity;

import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Builder
@Entity(value = "!!!testTransport", noClassnameStored = true)
public class TestTransportEntity implements PersistentEntity {
  @Id private String uuid;
}
