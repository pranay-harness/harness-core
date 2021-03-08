package io.harness.ng.core;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hibernate.validator.constraints.NotEmpty;

@Getter
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("org")
public class OrgScope extends ResourceScope {
  @NotEmpty String accountIdentifier;
  @NotEmpty String orgIdentifier;

  public OrgScope(String accountIdentifier, String orgIdentifier) {
    super("org");
    this.accountIdentifier = accountIdentifier;
    this.orgIdentifier = orgIdentifier;
  }
}
