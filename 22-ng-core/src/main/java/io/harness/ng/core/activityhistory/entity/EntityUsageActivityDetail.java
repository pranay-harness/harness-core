package io.harness.ng.core.activityhistory.entity;

import io.harness.ng.core.EntityDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

import javax.validation.constraints.NotNull;

@Data
@SuperBuilder
@NoArgsConstructor
@Entity(value = "ngActivity", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.ng.core.activity.EntityUsageActivityDetail")
@EqualsAndHashCode(callSuper = false)
public class EntityUsageActivityDetail extends NGActivity {
  @NotBlank String referredByEntityFQN;
  @NotBlank String referredByEntityType;
  @NotNull EntityDetail referredByEntity;
}
