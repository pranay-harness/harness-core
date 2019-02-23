package software.wings.beans.infrastructure;

import io.harness.annotation.HarnessExportableEntity;
import io.harness.annotation.NaturalKey;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.NameValuePair;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommand;

import java.util.List;

@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@Entity(value = "terraformConfig")
@HarnessExportableEntity
@Getter
public class TerraformfConfig extends Base {
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
