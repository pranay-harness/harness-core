package software.wings.beans.infrastructure;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.Map;

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

  private final Map<String, String> variables;
  private final Map<String, EncryptedDataDetail> encryptedVariables;

  @Indexed private final String entityId;
  @Indexed private final String workflowExecutionId;
}
