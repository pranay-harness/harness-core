package io.harness.cdng.environment.beans;

import io.harness.cdng.common.beans.Tag;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@CdUniqueIndex(
    name = "envNGIdx", fields = { @Field("accountId")
                                  , @Field("orgId"), @Field("projectId"), @Field("identifier") })
@Entity("environmentsNG")
@FieldNameConstants(innerTypeName = "EnvironmentKeys")
@Document("environmentsNG")
@TypeAlias("environmentsNG")
public class Environment implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @NonFinal @Id @org.mongodb.morphia.annotations.Id private String uuid;
  @NonFinal private String displayName;
  @NotEmpty private String identifier;
  @NotEmpty private EnvironmentType environmentType;
  @NonFinal private List<Tag> tags;
  private String accountId;
  private String orgId;
  private String projectId;
  @CreatedDate private long createdAt;
  @LastModifiedDate private long lastUpdatedAt;
}