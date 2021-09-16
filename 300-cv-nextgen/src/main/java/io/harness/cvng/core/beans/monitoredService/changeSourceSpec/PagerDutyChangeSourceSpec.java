package io.harness.cvng.core.beans.monitoredService.changeSourceSpec;

import io.harness.cvng.beans.change.ChangeSourceType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagerDutyChangeSourceSpec extends ChangeSourceSpec {
  @NonNull @NotEmpty String connectorRef;
  @NonNull @NotEmpty String pagerDutyServiceId;

  @Override
  public ChangeSourceType getType() {
    return ChangeSourceType.PAGER_DUTY;
  }
}
