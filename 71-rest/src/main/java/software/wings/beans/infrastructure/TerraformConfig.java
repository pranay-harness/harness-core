package software.wings.beans.infrastructure;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.NameValuePair;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommand;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@Entity(value = "terraformConfig")
@HarnessEntity(exportable = true)
@FieldNameConstants(innerTypeName = "TerraformConfigKeys")
public class TerraformConfig implements PersistentEntity, UuidAware, CreatedAtAware {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @Indexed @NotNull @SchemaIgnore protected String appId;
  @SchemaIgnore @Indexed private long createdAt;

  private final String sourceRepoSettingId;
  /**
   * This is generally represented by commit SHA in git.
   */
  private final String sourceRepoReference;

  /**
   * All variables of type TEXT & ENCRYPTED_TEXT.
   * Encrypted variables are not persisted in plain text. The uuid of the corresponding EncryptedRecord is stored.
   */
  private final List<NameValuePair> variables;
  private final List<NameValuePair> backendConfigs;
  private final List<String> targets;
  private final List<String> tfVarFiles;
  private final TerraformCommand command;

  @Indexed private final String entityId;
  private final String workflowExecutionId;
  private final String delegateTag;
}