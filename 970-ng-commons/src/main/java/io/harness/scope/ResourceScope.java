package io.harness.scope;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

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
