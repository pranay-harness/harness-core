package io.harness.accesscontrol.principals.serviceaccounts;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.principals.serviceaccounts.ServiceAccount;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
public interface ServiceAccountService {
  ServiceAccount createIfNotPresent(@NotNull @Valid ServiceAccount serviceAccount);

  PageResponse<ServiceAccount> list(@NotNull PageRequest pageRequest, @NotEmpty String scopeIdentifier);

  Optional<ServiceAccount> get(@NotEmpty String identifier, @NotEmpty String scopeIdentifier);

  Optional<ServiceAccount> deleteIfPresent(@NotEmpty String identifier, @NotEmpty String scopeIdentifier);
}
