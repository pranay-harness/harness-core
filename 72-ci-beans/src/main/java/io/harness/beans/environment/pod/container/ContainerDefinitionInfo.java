package io.harness.beans.environment.pod.container;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.ci.pod.ContainerResourceParams;

/**
 * Stores all details require to spawn container
 */

@Data
@Value
@Builder
public class ContainerDefinitionInfo implements UuidAccess {
  private String uuid = generateUuid();
  @NotEmpty private String name;
  @NotEmpty private ContainerImageDetails containerImageDetails;
  @NotEmpty private ContainerType containerType;
  @NotEmpty private ContainerResourceParams containerResourceParams;

  @Override
  public String getUuid() {
    return uuid;
  }
}
