package io.harness.ng.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import com.mongodb.lang.NonNull;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.Field;
import io.harness.ng.core.NGAccountAccess;
import io.harness.ng.core.models.Invite.InviteKeys;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.Date;

@Data
@Builder
@FieldNameConstants(innerTypeName = "InviteKeys")
@Entity(value = "invites", noClassnameStored = true)
@CdUniqueIndex(name = "unique_ng_invite",
    fields =
    {
      @Field(InviteKeys.accountIdentifier)
      , @Field(InviteKeys.orgIdentifier), @Field(InviteKeys.projectIdentifier), @Field(InviteKeys.email),
          @Field(InviteKeys.role)
    })
@Document("invites")
@TypeAlias("invites")
public class Invite implements PersistentEntity, NGAccountAccess {
  @Trimmed @NotEmpty String accountIdentifier;
  @Id @org.mongodb.morphia.annotations.Id @EntityIdentifier String uuid;
  @Trimmed @NotEmpty String orgIdentifier;
  @Trimmed @NotEmpty String projectIdentifier;
  @Wither @NotEmpty @EntityName @Email String email;
  @NotEmpty Role role;
  String name;
  @NonNull InviteType inviteType;
  @NonNull Boolean approved;
  @Wither @Version Long version;

  @JsonIgnore
  @SchemaIgnore
  @FdTtlIndex
  @Builder.Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(14).toInstant());
}
