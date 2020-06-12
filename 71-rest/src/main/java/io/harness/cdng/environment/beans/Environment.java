package io.harness.cdng.environment.beans;

import io.harness.beans.EmbeddedUser;
import io.harness.cdng.common.beans.Tag;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.List;

@Data
@Builder
@Indexes({
  @Index(options = @IndexOptions(name = "envNGIdx", unique = true), fields = {
    @Field("accountId"), @Field("orgId"), @Field("projectId"), @Field("identifier")
  })
})
@Entity("environmentsNG")
@FieldNameConstants(innerTypeName = "EnvironmentKeys")
public class Environment
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, CreatedByAware, UpdatedByAware {
  @NonFinal @Id private String uuid;
  @NonFinal private String displayName;
  @NotEmpty private String identifier;
  @NotEmpty private EnvironmentType environmentType;
  @NonFinal private List<Tag> tags;
  private String accountId;
  private String orgId;
  private String projectId;
  private long createdAt;
  private long lastUpdatedAt;
  private EmbeddedUser createdBy;
  private EmbeddedUser lastUpdatedBy;
}
