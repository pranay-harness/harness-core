package software.wings.beans.infrastructure;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;

import com.github.reinert.jjschema.SchemaIgnore;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@Entity(value = "armRollbackConfig")
@HarnessEntity(exportable = true)
@FieldNameConstants(innerTypeName = "ARMRollbackConfigKeys")
public class ARMRollbackConfig implements PersistentEntity, UuidAware, CreatedAtAware, AccountAccess {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @FdIndex @NotNull @SchemaIgnore protected String appId;
  @SchemaIgnore @FdIndex private long createdAt;
  @FdIndex private String accountId;
  @FdIndex private String entityId;
  private String workflowExecutionId;

  private String inlineTemplateForRollback;
  private String inlineVariablesForRollback;

  private String templateConnectorId;
  private String templateCommitId;
  private String templateFilePath;

  private String variablesConnectorId;
  private String variablesCommitId;
  private String variablesFilePath;
}
