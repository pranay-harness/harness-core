package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.AccountAccess;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Preference.PreferenceKeys;

@Data
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "preferenceType")
@JsonSubTypes({
  @Type(value = DeploymentPreference.class, name = "DEPLOYMENT_PREFERENCE")
  , @Type(value = AuditPreference.class, name = "AUDIT_PREFERENCE")
})
@FieldNameConstants(innerTypeName = "PreferenceKeys")

@CdIndex(name = "preference_index",
    fields = { @Field(PreferenceKeys.accountId)
               , @Field(PreferenceKeys.userId), @Field(PreferenceKeys.name) })

@EqualsAndHashCode(callSuper = false)
@Entity(value = "preferences")
@HarnessEntity(exportable = true)

public abstract class Preference extends Base implements AccountAccess {
  @NotEmpty private String name;
  @NotEmpty private String accountId;
  @NotEmpty private String userId;
  private String preferenceType;

  public Preference(String preferenceType) {
    this.preferenceType = preferenceType;
  }

  @UtilityClass
  public static final class PreferenceKeys {
    // Temporary
    public static final String createdAt = "createdAt";
    public static final String uuid = "uuid";
  }
}
