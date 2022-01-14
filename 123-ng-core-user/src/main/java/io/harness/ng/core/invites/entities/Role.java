package io.harness.ng.core.invites.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
import io.harness.ng.core.NGAccountAccess;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Data
@Builder
@FieldNameConstants(innerTypeName = "RolesKeys")
@OwnedBy(PL)
public class Role implements NGAccountAccess {
  @Wither @Id @org.mongodb.morphia.annotations.Id String id;
  @Trimmed @NotEmpty String accountIdentifier;
  @Trimmed @NotEmpty String orgIdentifier;
  @Trimmed @NotEmpty String projectIdentifier;
  @NotEmpty String name;
  @Wither @Version Long version;
}
