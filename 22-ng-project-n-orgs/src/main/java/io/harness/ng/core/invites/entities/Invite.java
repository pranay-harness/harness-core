package io.harness.ng.core.invites.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.Field;
import io.harness.ng.core.NGAccountAccess;
import io.harness.ng.core.invites.entities.Invite.InviteKeys;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import com.mongodb.lang.NonNull;
import java.time.OffsetDateTime;
import java.util.Date;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.codehaus.jackson.annotate.JsonProperty;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "InviteKeys")
@Entity(value = "invites", noClassnameStored = true)
@CdIndex(name = "ng_invite_account_org_project_identifiers_email_role_deleted",
    fields =
    {
      @Field(InviteKeys.deleted)
      , @Field(InviteKeys.accountIdentifier), @Field(InviteKeys.orgIdentifier), @Field(InviteKeys.projectIdentifier),
          @Field(InviteKeys.email), @Field(InviteKeys.role)
    })
@Document("invites")
@TypeAlias("invites")
@OwnedBy(PL)
public class Invite implements PersistentEntity, NGAccountAccess {
  @Trimmed @NotEmpty String accountIdentifier;
  @Wither @Id @org.mongodb.morphia.annotations.Id @EntityIdentifier String id;
  @Trimmed @NotEmpty String orgIdentifier;
  @Trimmed @NotEmpty String projectIdentifier;
  @Wither @NotEmpty @EntityName @Email String email;
  @Wither @NotEmpty Role role;
  String name;
  @NonNull InviteType inviteType;
  @CreatedDate Long createdAt;
  @Wither @Version Long version;
  @Trimmed String inviteToken;
  @NonNull Boolean approved;
  @Builder.Default Boolean deleted = Boolean.FALSE;

  @JsonIgnore
  @SchemaIgnore
  @FdTtlIndex
  @Builder.Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(30).toInstant());

  @OwnedBy(PL)
  public enum InviteType {
    @JsonProperty("USER_INITIATED_INVITE") USER_INITIATED_INVITE("USER_INITIATED_INVITE"),
    @JsonProperty("ADMIN_INITIATED_INVITE") ADMIN_INITIATED_INVITE("ADMIN_INITIATED_INVITE");

    private String type;
    InviteType(String type) {
      this.type = type;
    }

    public String getType() {
      return type;
    }
  }
}
