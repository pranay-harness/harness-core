/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories;

import io.harness.ci.beans.entities.CIBuild;

import java.util.Optional;

public interface BuildDao {
  Optional<CIBuild> findByKey(String key);
}
