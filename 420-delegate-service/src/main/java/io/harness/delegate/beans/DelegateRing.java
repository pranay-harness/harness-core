package io.harness.delegate.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@FieldNameConstants(innerTypeName = "DelegateRingKeys")
@Entity(value = "delegateRing", noClassnameStored = true)
@HarnessEntity(exportable = false)
@OwnedBy(HarnessTeam.DEL)
public class DelegateRing implements PersistentEntity, UuidAware {
  @Id @NotNull private String uuid;

  @NotEmpty private String ringName;
  @NotEmpty private String delegateImageTag;
  @NotEmpty private String upgraderImageTag;
}
