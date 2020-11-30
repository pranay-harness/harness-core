package io.harness.pms.sdk;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.pms.steps.StepInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "PmsSdkInstanceKeys")
@Entity(value = "pmsSdkInstances", noClassnameStored = true)
@Document("pmsSdkInstances")
@TypeAlias("pmsSdkInstances")
@HarnessEntity(exportable = false)
public class PmsSdkInstance implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id @org.mongodb.morphia.annotations.Id String uuid;

  @NotNull @FdUniqueIndex String name;
  Map<String, Set<String>> supportedTypes;
  List<StepInfo> supportedSteps;

  @SchemaIgnore @FdIndex @CreatedDate private long createdAt;
  @SchemaIgnore @NotNull @LastModifiedDate private long lastUpdatedAt;
  @Version Long version;
}
