package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdUniqueIndex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;

@OwnedBy(PL)
@Data
@SuperBuilder
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "SecretKey")
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "SecretKey", noClassnameStored = true)
public class SecretKey {
  @FdUniqueIndex private String uuid;
  private String key;
}
