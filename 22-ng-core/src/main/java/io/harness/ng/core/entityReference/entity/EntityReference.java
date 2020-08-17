package io.harness.ng.core.entityReference.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.ng.core.NGAccountAccess;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "EntityReferenceKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("entityReference")
public class EntityReference implements PersistentEntity, NGAccountAccess {
  @Id @org.mongodb.morphia.annotations.Id String id;
  @NotBlank String accountIdentifier;
  @NotBlank String referredEntityFQN;
  @NotBlank String referredEntityType;
  @NotBlank String referredByEntityFQN;
  @NotBlank String referredByEntityType;
  // todo @deepak: Add the support for setup usage
}
