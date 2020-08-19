package io.harness.ng.core.models;

import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.Field;
import io.harness.ng.core.NGAccountAccess;
import io.harness.ng.core.models.Role.RolesKeys;
import io.harness.persistence.PersistentEntity;
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
@FieldNameConstants(innerTypeName = "RolesKeys")
@CdUniqueIndex(name = "unique_accountIdentifier_orgIdentifier_projectIdentifier_roleIdentifier",
    fields =
    { @Field(RolesKeys.accountIdentifier)
      , @Field(RolesKeys.projectIdentifier), @Field(RolesKeys.orgIdentifier) })
@Entity(value = "roles", noClassnameStored = true)
//@CdIndex(name = "projRolesIdx",
//        fields = { @Field(RolesKeys.projectIdentifier), @Field(RolesKeys.inviteIdentifier) })
@Document("roles")
@TypeAlias("roles")
public class Role implements PersistentEntity, NGAccountAccess {
  @Wither @Id @org.mongodb.morphia.annotations.Id String id;
  @Trimmed @NotEmpty String accountIdentifier;
  @Trimmed @NotEmpty String orgIdentifier;
  @Trimmed @NotEmpty String projectIdentifier;
  @NotEmpty String name;
  @Wither @Version Long version;
}
