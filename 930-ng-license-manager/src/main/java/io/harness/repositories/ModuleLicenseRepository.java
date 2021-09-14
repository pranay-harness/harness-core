/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories;

import io.harness.ModuleType;
import io.harness.annotation.HarnessRepo;
import io.harness.licensing.entities.modules.ModuleLicense;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
public interface ModuleLicenseRepository extends CrudRepository<ModuleLicense, String> {
  List<ModuleLicense> findByAccountIdentifierAndModuleType(String accountIdentifier, ModuleType moduleType);
  List<ModuleLicense> findByAccountIdentifier(String accountIdentifier);
}
