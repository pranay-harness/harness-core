package io.harness.audit.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.ResourceScope;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@OwnedBy(PL)
@Data
@Builder(builderClassName = "Builder")
@JsonInclude(NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ResourceScopeKeys")
public class ResourceScopeDTO {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  @Size(max = 2) Map<String, String> labels;

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

  public static ResourceScopeDTO fromResourceScope(ResourceScope resourceScope) {
    switch (resourceScope.getScope()) {
      case "account":
        return ResourceScopeDTO.builder()
            .accountIdentifier(((AccountScope) resourceScope).getAccountIdentifier())
            .build();
      case "org":
        return ResourceScopeDTO.builder()
            .accountIdentifier(((OrgScope) resourceScope).getAccountIdentifier())
            .orgIdentifier(((OrgScope) resourceScope).getOrgIdentifier())
            .build();
      case "project":
        return ResourceScopeDTO.builder()
            .accountIdentifier(((ProjectScope) resourceScope).getAccountIdentifier())
            .orgIdentifier(((ProjectScope) resourceScope).getOrgIdentifier())
            .projectIdentifier(((ProjectScope) resourceScope).getProjectIdentifier())
            .build();
      default:
        throw new InvalidArgumentsException(String.format("Illegal scope of resource %s", resourceScope.getScope()));
    }
  }
}
