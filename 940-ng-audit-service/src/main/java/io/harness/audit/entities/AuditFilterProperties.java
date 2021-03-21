package io.harness.audit.entities;

import io.harness.ModuleType;
import io.harness.audit.beans.Principal;
import io.harness.filter.entity.FilterProperties;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuditFilterProperties extends FilterProperties {
  List<ResourceScope> scopes;
  List<Resource> resources;

  List<ModuleType> moduleTypes;
  List<String> actions;
  List<String> environmentIdentifiers;
  List<Principal> principals;

  Long startTime;
  Long endTime;
}
