package io.harness.beans.environment.pod.container;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Value
@Builder
public class CIContainerDefinitionInfo implements UuidAccess {
  private String uuid = generateUuid();
  @NotEmpty private String settingId;
  private CIContainerImageDetails ciImageDetailsInfo;
  private CIContainerType containerType;

  @Override
  public String getUuid() {
    return uuid;
  }
}
