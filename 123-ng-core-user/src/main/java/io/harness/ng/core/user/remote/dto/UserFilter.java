package io.harness.ng.core.user.remote.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@ApiModel(value = "UserFilter")
@OwnedBy(PL)
public class UserFilter {
  private String searchTerm;
  private Set<String> identifiers;
  @Builder.Default private ParentFilter parentFilter = ParentFilter.NO_PARENT_SCOPES;

  public ParentFilter getParentFilter() {
    return parentFilter == null ? ParentFilter.NO_PARENT_SCOPES : parentFilter;
  }

  public enum ParentFilter { NO_PARENT_SCOPES, INCLUDE_PARENT_SCOPES, STRICTLY_PARENT_SCOPES }
}
