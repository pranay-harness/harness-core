package io.harness.cvng.core.beans.monitoredService.changeSourceSpec;

import io.harness.data.validator.EntityIdentifier;

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
public abstract class ChangeSourceWithConnectorSpec extends ChangeSourceSpec {
  @NonNull @NotEmpty @EntityIdentifier(allowScoped = true) String connectorRef;

  @Override
  public boolean connectorPresent() {
    return true;
  }
}
