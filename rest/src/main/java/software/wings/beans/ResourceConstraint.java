package software.wings.beans;

import io.harness.distribution.constraint.Constraint;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

@Entity(value = "resourceConstraint", noClassnameStored = true)
@Indexes(@Index(
    options = @IndexOptions(unique = true, name = "uniqueName"), fields = { @Field("accountId")
                                                                            , @Field("name") }))
@EqualsAndHashCode(callSuper = false)
@Data
@NoArgsConstructor
public class ResourceConstraint extends Base {
  public static final String NAME_KEY = "name";

  @NotEmpty private String accountId;
  private String name;
  private int capacity;
  private Constraint.Strategy strategy;

  @Builder
  private ResourceConstraint(String uuid, String accountId, String name, int capacity, Constraint.Strategy strategy) {
    setUuid(uuid);
    setAppId(GLOBAL_APP_ID);
    this.accountId = accountId;
    this.name = name;
    this.capacity = capacity;
    this.strategy = strategy;
  }
}
