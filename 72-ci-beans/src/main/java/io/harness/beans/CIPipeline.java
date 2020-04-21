package io.harness.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.repo.RepoConfiguration;
import io.harness.beans.stages.CIStageInfo;
import io.harness.data.validator.EntityName;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.NameAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
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
//@StoreIn("harnessci")
public class CIPipeline implements NameAccess, PersistentEntity, UuidAccess, AccountAccess {
  @NotNull @EntityName private String name;

  private EmbeddedUser createdBy;
  private long createdAt;

  private String description;

  // Todo: Store stage in different collection
  @Indexed private List<CIStageInfo> linkedStages = new ArrayList<>();

  @Indexed private String accountId;

  @Id private String uuid;

  @NotNull private RepoConfiguration RepoConfiguration;
}
