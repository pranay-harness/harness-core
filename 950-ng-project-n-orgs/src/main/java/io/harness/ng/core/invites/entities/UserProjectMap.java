package io.harness.ng.core.invites.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.ng.core.invites.entities.UserProjectMap.UserProjectMapKeys;
import io.harness.persistence.PersistentEntity;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "UserProjectMapKeys")
@NgUniqueIndex(name = "uniqueUserProjectMap",
    fields =
    {
      @Field(UserProjectMapKeys.userId)
      , @Field(UserProjectMapKeys.accountIdentifier), @Field(UserProjectMapKeys.orgIdentifier),
          @Field(UserProjectMapKeys.projectIdentifier), @Field(UserProjectMapKeys.roles)
    })
@Entity(value = "userProjectMaps", noClassnameStored = true)
@Document("userProjectMaps")
@TypeAlias("userProjectMaps")
@OwnedBy(PL)
public class UserProjectMap implements PersistentEntity {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  @Trimmed @NotEmpty String userId;
  @Trimmed @NotEmpty String accountIdentifier;
  @Trimmed @NotEmpty String orgIdentifier;
  @Trimmed @NotEmpty String projectIdentifier;
  @Trimmed @NotEmpty List<Role> roles;
  @Wither @Version Long version;
}
