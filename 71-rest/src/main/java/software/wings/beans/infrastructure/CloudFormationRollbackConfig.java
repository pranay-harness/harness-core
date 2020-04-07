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

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@Entity(value = "cloudFormationRollbackConfig")
@HarnessEntity(exportable = true)
@FieldNameConstants(innerTypeName = "CloudFormationRollbackConfigKeys")
public class CloudFormationRollbackConfig implements PersistentEntity, UuidAware, CreatedAtAware {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @Indexed private String accountId;
  @Indexed @NotNull @SchemaIgnore protected String appId;
  @SchemaIgnore @Indexed private long createdAt;

  private String url;
  private String body;
  private String createType;
  private List<NameValuePair> variables;

  private String region;
  private String awsConfigId;
  private String customStackName;
  private String workflowExecutionId;
  @Indexed private String entityId;
}