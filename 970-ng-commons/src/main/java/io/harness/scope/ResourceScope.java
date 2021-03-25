package io.harness.scope;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;

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
    return hasSome(accountIdentifier) && hasSome(orgIdentifier) && hasNone(projectIdentifier);
  }

  @JsonIgnore
  public boolean isProjectScoped() {
    return hasSome(accountIdentifier) && hasSome(orgIdentifier) && hasSome(projectIdentifier);
  }

  @JsonIgnore
  public boolean isAccountScoped() {
    return hasSome(accountIdentifier) && hasNone(orgIdentifier) && hasNone(projectIdentifier);
  }

  public static ResourceScope fromResourceScope(io.harness.ng.core.ResourceScope resourceScope) {
    switch (resourceScope.getScope()) {
      case "account":
        return ResourceScope.builder().accountIdentifier(((AccountScope) resourceScope).getAccountIdentifier()).build();
      case "org":
        return ResourceScope.builder()
            .accountIdentifier(((OrgScope) resourceScope).getAccountIdentifier())
            .orgIdentifier(((OrgScope) resourceScope).getOrgIdentifier())
            .build();
      case "project":
        return ResourceScope.builder()
            .accountIdentifier(((ProjectScope) resourceScope).getAccountIdentifier())
            .orgIdentifier(((ProjectScope) resourceScope).getOrgIdentifier())
            .projectIdentifier(((ProjectScope) resourceScope).getProjectIdentifier())
            .build();
      default:
        throw new IllegalArgumentException("Illegal scope of resource {}".format(resourceScope.getScope()));
    }
  }
}
