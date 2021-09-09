package io.harness.cvng.core.beans.monitoredService.changeSourceSpec;

import io.harness.cvng.core.types.ChangeSourceType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesChangeSourceSpec extends ChangeSourceSpec {
  @NotEmpty String connectorRef;

  @Override
  public ChangeSourceType getType() {
    return ChangeSourceType.KUBERNETES;
  }
}
