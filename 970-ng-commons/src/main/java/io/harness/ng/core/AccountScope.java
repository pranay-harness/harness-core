package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
@Getter
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("account")
public class AccountScope extends ResourceScope {
  @NotEmpty String accountIdentifier;

  public AccountScope(String accountIdentifier) {
    super("account");
    this.accountIdentifier = accountIdentifier;
  }
}
