package software.wings.beans.infrastructure;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.annotation.NaturalKey;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@HarnessExportableEntity
public class TerraformfConfig implements PersistentEntity, UuidAware, CreatedAtAware {
  public static final String APP_ID_KEY = "appId";

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @Indexed @NotNull @SchemaIgnore @NaturalKey protected String appId;
  @SchemaIgnore @Indexed private long createdAt;

  public static final String ENTITY_ID_KEY = "entityId";
  public static final String WORKFLOW_EXECUTION_ID_KEY = "workflowExecutionId";

  @NaturalKey private final String sourceRepoSettingId;
  /**
   * This is generally represented by commit SHA in git.
   */
  @NaturalKey private final String sourceRepoReference;

  /**
   * All variables of type TEXT & ENCRYPTED_TEXT.
   * Encrypted variables are not persisted in plain text. The uuid of the corresponding EncryptedRecord is stored.
   */
  private final List<NameValuePair> variables;
  private final List<NameValuePair> backendConfigs;
  private final List<String> targets;
  private final TerraformCommand command;

  @Indexed @NaturalKey private final String entityId;
  @NaturalKey private final String workflowExecutionId;
}
