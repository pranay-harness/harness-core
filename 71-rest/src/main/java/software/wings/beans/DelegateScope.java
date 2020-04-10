package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.DelegateScope.DelegateScopeKeys;
import software.wings.beans.Environment.EnvironmentType;

import java.util.List;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@FieldNameConstants(innerTypeName = "DelegateScopeKeys")
@Entity(value = "delegateScopes", noClassnameStored = true)
@HarnessEntity(exportable = true)
// TODO: we should enforce name uniqueness, if we would like to use scopes long term, for the time being
//       drop the uniqueness until we figure out what is the plan
@Indexes(@Index(options = @IndexOptions(name = "uniqueName" /*, unique = true*/),
    fields = { @Field(value = DelegateScopeKeys.accountId)
               , @Field(value = DelegateScopeKeys.name) }))
public class DelegateScope
    implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @Indexed private long createdAt;

  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;

  @NotEmpty private String accountId;
  private String name;
  private List<TaskGroup> taskTypes;
  private List<EnvironmentType> environmentTypes;
  private List<String> applications;
  private List<String> environments;
  private List<String> serviceInfrastructures;
  private List<String> services;
  private List<String> infrastructureDefinitions;

  public boolean isValid() {
    return (isNotEmpty(taskTypes)) || (isNotEmpty(environmentTypes)) || (isNotEmpty(applications))
        || (isNotEmpty(environments)) || (isNotEmpty(serviceInfrastructures)) || (isNotEmpty(infrastructureDefinitions))
        || (isNotEmpty(services));
  }
}
