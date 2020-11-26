package io.harness.delegate.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.delegate.beans.DelegateProfile.DelegateProfileKeys;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@Entity(value = "delegateProfiles", noClassnameStored = true)
@HarnessEntity(exportable = true)
@FieldNameConstants(innerTypeName = "DelegateProfileKeys")
@NgUniqueIndex(name = "uniqueName",
    fields = { @Field(value = DelegateProfileKeys.accountId)
               , @Field(value = DelegateProfileKeys.name) })
public final class DelegateProfile implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware,
                                              UpdatedAtAware, UpdatedByAware, AccountAccess {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  @NotEmpty private String accountId;

  @NotEmpty private String name;
  private String description;

  private boolean primary;

  private boolean approvalRequired;

  private String startupScript;

  private List<DelegateProfileScopingRule> scopingRules;

  private List<String> selectors;

  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @FdIndex private long createdAt;

  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;
}
