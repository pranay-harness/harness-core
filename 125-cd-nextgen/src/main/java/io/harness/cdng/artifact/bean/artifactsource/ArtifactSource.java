package io.harness.cdng.artifact.bean.artifactsource;

import io.harness.annotation.HarnessEntity;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.github.reinert.jjschema.SchemaIgnore;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Artifacts Streams like Docker Hub, Nexus, GCR, etc.
 * This is the resolved artifact stream, can be used for Artifact Collection, etc.has
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@NgUniqueIndex(name = "uniqueHash", fields = { @Field("uniqueHash") })
@FieldNameConstants(innerTypeName = "ArtifactSourceKeys")
@Entity(value = "artifactSourceNG")
@Document("artifactSourceNG")
@TypeAlias("artifactSourceNG")
@HarnessEntity(exportable = true)
public abstract class ArtifactSource
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id @org.mongodb.morphia.annotations.Id private String uuid;
  @NotNull private String accountId;
  /** It gives the artifact source type.*/
  @NotNull private ArtifactSourceType sourceType;
  /** This uniquely identifies one artifact stream based on its parameters.*/
  @NotNull private String uniqueHash;

  @SchemaIgnore @FdIndex @CreatedDate private long createdAt;
  @SchemaIgnore @NotNull @LastModifiedDate private long lastUpdatedAt;
  @Version Long version;
}
