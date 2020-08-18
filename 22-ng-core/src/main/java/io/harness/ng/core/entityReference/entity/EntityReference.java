package io.harness.ng.core.entityReference.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.EmbeddedUser;
import io.harness.ng.core.NGAccountAccess;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "EntityReferenceKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("entityReference")
@TypeAlias("io.harness.ng.core.entityReference.entity.EntityReference")
public class EntityReference implements PersistentEntity, NGAccountAccess {
  @Id @org.mongodb.morphia.annotations.Id String id;
  @NotBlank String accountIdentifier;
  @NotBlank String referredEntityName;
  @NotBlank String referredEntityFQN;
  @NotBlank String referredEntityType;
  @NotBlank String referredByEntityName;
  @NotBlank String referredByEntityFQN;
  @NotBlank String referredByEntityType;
  // todo @deepak: Add the support for activity
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;
  @CreatedBy private EmbeddedUser createdBy;
  @LastModifiedBy private EmbeddedUser lastUpdatedBy;
}
