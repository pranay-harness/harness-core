package io.harness.springdata;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.Principal;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;

@OwnedBy(PL)
public class AuditorAwareImpl implements AuditorAware<Principal> {
  @Override
  public Optional<Principal> getCurrentAuditor() {
    return Optional.ofNullable(SourcePrincipalContextBuilder.getSourcePrincipal());
  }
}