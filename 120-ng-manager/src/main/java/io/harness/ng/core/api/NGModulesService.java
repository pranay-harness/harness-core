package io.harness.ng.core.api;

import io.harness.ModuleType;

import java.util.List;

public interface NGModulesService {
  List<ModuleType> getEnabledModules(String accountIdentifier);
}
