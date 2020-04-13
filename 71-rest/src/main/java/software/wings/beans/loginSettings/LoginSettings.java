package software.wings.beans.loginSettings;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldNameConstants(innerTypeName = "LoginSettingKeys")
@Entity(value = "loginSettings")
@HarnessEntity(exportable = true)
public class LoginSettings implements PersistentEntity, UuidAware, UpdatedAtAware, UpdatedByAware, AccountAccess {
  @Id @NotNull @SchemaIgnore private String uuid;

  @NotNull private String accountId;
  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore private long lastUpdatedAt;

  @NotNull @Valid private UserLockoutPolicy userLockoutPolicy;
  @NotNull @Valid private PasswordExpirationPolicy passwordExpirationPolicy;
  @NotNull @Valid private PasswordStrengthPolicy passwordStrengthPolicy;
}
