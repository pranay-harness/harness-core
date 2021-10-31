package io.harness.cvng.core.beans.monitoredService.changeSourceSpec;

import io.harness.cvng.beans.change.ChangeSourceType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
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
public class HarnessCDCurrentGenChangeSourceSpec extends ChangeSourceSpec {
  @NotNull @NotEmpty String harnessApplicationId;
  @NotNull @NotEmpty String harnessServiceId;
  @NotNull @NotEmpty String harnessEnvironmentId;

  @Override
  public ChangeSourceType getType() {
    return ChangeSourceType.HARNESS_CD_CURRENT_GEN;
  }

  @Override
  public boolean connectorPresent() {
    return false;
  }
}
