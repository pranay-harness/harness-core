package io.harness.persistence.converters;

import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Id;

import java.time.Duration;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "DurationTestEntityKeys")
@Converters({DurationConverter.class})
public class DurationTestEntity implements PersistentEntity, UuidAccess {
  @Id String uuid;
  Duration testDuration;
}
