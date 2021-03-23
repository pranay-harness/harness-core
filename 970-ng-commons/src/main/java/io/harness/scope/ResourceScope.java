package io.harness.scope;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
@Data
@Builder(builderClassName = "Builder")
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ResourceScopeKeys")
public class ResourceScope {
  @NotNull @NotEmpty String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;

  @JsonIgnore
  public boolean isOrgScoped() {
    return isNotEmpty(accountIdentifier) && isNotEmpty(orgIdentifier) && isEmpty(projectIdentifier);
  }

  @JsonIgnore
  public boolean isProjectScoped() {
    return isNotEmpty(accountIdentifier) && isNotEmpty(orgIdentifier) && isNotEmpty(projectIdentifier);
  }

  @JsonIgnore
  public boolean isAccountScoped() {
    return isNotEmpty(accountIdentifier) && isEmpty(orgIdentifier) && isEmpty(projectIdentifier);
  }
}
