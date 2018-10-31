package software.wings.beans.infrastructure;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.NameValuePair;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Builder(toBuilder = true)
@Entity(value = "terraformConfig")
@Getter
public class TerraformfConfig extends Base {
  public static final String ENTITY_ID_KEY = "entityId";
  public static final String WORKFLOW_EXECUTION_ID_KEY = "workflowExecutionId";

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

  @Indexed private final String entityId;
  @Indexed private final String workflowExecutionId;
}
