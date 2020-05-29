package io.harness.beans;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.repo.RepoConfiguration;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import io.harness.yaml.core.nonyaml.NonYamlInfo;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class CIPipelineEntityInfo implements NonYamlInfo, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  private EmbeddedUser createdBy;
  private long createdAt;

  private long lastUpdatedAt;

  @Indexed private String accountId;

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  @NotNull private RepoConfiguration repoConfiguration;
}
