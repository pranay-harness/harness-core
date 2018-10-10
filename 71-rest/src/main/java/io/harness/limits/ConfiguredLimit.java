package io.harness.limits;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.limits.lib.Limit;
import io.harness.validation.Update;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import javax.validation.constraints.NotNull;

/**
 * Documents with accountId = GLOBAL_ACCOUNT_ID are treated as default limits for all accounts.
 * To override limits for an account, there should be an entry present with that account Id.
 */
@Getter
@EqualsAndHashCode(exclude = "id")
@ToString
@Entity(value = "allowedLimits", noClassnameStored = true)
@Indexes(
    @Index(fields = { @Field("key")
                      , @Field("accountId") }, options = @IndexOptions(name = "key_idx", unique = true)))
public class ConfiguredLimit<T extends Limit> {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private ObjectId id;

  private String accountId;
  private String key;
  private T limit;

  public ConfiguredLimit(String accountId, T limit, ActionType actionType) {
    this.accountId = accountId;
    this.key = actionType.toString();
    this.limit = limit;
  }

  public T getLimit() {
    return limit;
  }

  // morphia expects an no-args constructor
  private ConfiguredLimit() {}
}
