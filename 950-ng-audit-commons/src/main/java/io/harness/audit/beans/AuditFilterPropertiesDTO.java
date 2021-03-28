package io.harness.audit.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.filter.FilterConstants.AUDIT_FILTER;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.ng.core.Resource;
import io.harness.scope.ResourceScope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AUDIT_FILTER)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("AuditFilterProperties")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuditFilterPropertiesDTO extends FilterPropertiesDTO {
  List<ResourceScope> scopes;
  List<Resource> resources;

  List<ModuleType> modules;
  List<Action> actions;
  List<String> environmentIdentifiers;
  List<Principal> principals;

  Long startTime;
  Long endTime;

  @Override
  public FilterType getFilterType() {
    return FilterType.AUDIT;
  }
}
