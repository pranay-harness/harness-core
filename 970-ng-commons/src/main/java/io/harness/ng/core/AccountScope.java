package io.harness.ng.core;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hibernate.validator.constraints.NotEmpty;

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
