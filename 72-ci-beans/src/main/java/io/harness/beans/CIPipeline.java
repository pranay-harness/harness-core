package io.harness.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.beans.repo.RepoConfiguration;
import io.harness.beans.stages.StageInfo;
import io.harness.data.validator.EntityName;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Builder
@Entity(value = "cipipeline", noClassnameStored = true)
@HarnessEntity(exportable = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@StoreIn("harnessci")
public class CIPipeline implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @NotNull @EntityName private String name;

  private EmbeddedUser createdBy;
  private long createdAt;

  private long lastUpdatedAt;
  private String description;

  // Todo: Store stage in different collection
  @Indexed private List<StageInfo> linkedStages = new ArrayList<>();

  @Indexed private String accountId;

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  @NotNull private RepoConfiguration repoConfiguration;
}
