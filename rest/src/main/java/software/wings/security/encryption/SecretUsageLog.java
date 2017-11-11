package software.wings.security.encryption;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;

/**
 * Created by rsingh on 10/27/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(value = "secretUsageLogs", noClassnameStored = true)
public class SecretUsageLog extends Base {
  @NotEmpty @Indexed private String encryptedDataId;

  @NotEmpty @Indexed private String accountId;

  @NotEmpty private String workflowExecutionId;

  @NotEmpty private String envId;

  private String entityName;

  // not stored
  @Transient private transient String workflowExecutionName;
}
