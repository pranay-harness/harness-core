package software.wings.beans.security;

import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.github.reinert.jjschema.SchemaIgnore;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@Entity(value = "alerts")
public class AccessRequest implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id @NotNull @SchemaIgnore private String uuid;
  @SchemaIgnore private long createdAt;
  @SchemaIgnore @NotNull private long lastUpdatedAt;
  @NotNull String accountId;
  @NotNull String harnessUserGroupId;
  private long accessStartAt;
  private long accessEndAt;
  private boolean accessActive;
}
