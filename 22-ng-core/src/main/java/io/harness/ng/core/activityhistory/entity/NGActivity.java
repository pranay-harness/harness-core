package io.harness.ng.core.activityhistory.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.ng.core.NGAccountAccess;
import io.harness.persistence.PersistentEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

@Data
@SuperBuilder
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "ActivityHistoryEntityKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("ngActivity")
public abstract class NGActivity implements PersistentEntity, NGAccountAccess {
  @Id @org.mongodb.morphia.annotations.Id String id;
  @NotBlank String accountIdentifier;
  String referredEntityOrgIdentifier;
  String referredEntityProjectIdentifier;
  @NotBlank String referredEntityIdentifier;
  @NotNull String referredEntityScope;
  String referredEntityFQN;
  @NotNull String referredEntityType;
  @NotNull String type;
  String activityStatus;
  @NotNull long activityTime;
  String description;
  String errorMessage;
}
