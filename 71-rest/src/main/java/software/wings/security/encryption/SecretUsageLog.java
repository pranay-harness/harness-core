package software.wings.security.encryption;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;

import java.time.OffsetDateTime;
import java.util.Date;

/**
 * Created by rsingh on 10/27/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
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

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());
}
