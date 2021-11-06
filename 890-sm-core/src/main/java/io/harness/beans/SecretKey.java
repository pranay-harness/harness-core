package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.crypto.spec.SecretKeySpec;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;

@OwnedBy(PL)
@Data
@SuperBuilder
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "SecretKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "SecretKey", noClassnameStored = true)
public class SecretKey {
  @Id @org.mongodb.morphia.annotations.Id private String uuid;
  private SecretKeySpec secretKeySpec;
}
