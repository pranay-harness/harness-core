package io.harness.ng.core.activityhistory.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Data
@SuperBuilder
@NoArgsConstructor
@Entity(value = "ngActivity", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.ng.core.activity.EntityUsageActivityDetail")
@EqualsAndHashCode(callSuper = false)
public class EntityUsageActivityDetail extends NGActivity {
  String referredByEntityOrgIdentifier;
  String referredByEntityProjectIdentifier;
  @NotBlank String referredByEntityIdentifier;
  @NotEmpty String referredByEntityScope;
  @NotBlank String referredByEntityFQN;
  @NotBlank String referredByEntityType;
}
