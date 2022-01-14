package io.harness.accesscontrol.support.persistence;

import io.harness.accesscontrol.support.SupportPreference;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
public interface SupportPreferenceDao {
  Optional<SupportPreference> get(@NotEmpty String accountIdentifier);
  SupportPreference upsert(@NotNull @Valid SupportPreference supportPreference);
}
