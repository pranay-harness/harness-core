package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"runtimeParameters"})
@EqualsAndHashCode
@Entity(value = "secretManagerRuntimeParameters", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "SecretManagerRuntimeParametersKeys")
public class SecretManagerRuntimeParameters implements PersistentEntity, UuidAware {
  @Id private String uuid;
  private String secretManagerId;
  @FdIndex private String executionId;
  private String runtimeParameters;
  private String accountId;

  @Override
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public String getUuid() {
    return this.uuid;
  }
}
