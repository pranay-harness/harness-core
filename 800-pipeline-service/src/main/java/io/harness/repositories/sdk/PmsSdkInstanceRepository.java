/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.repositories.sdk;

import io.harness.annotation.HarnessRepo;
import io.harness.pms.sdk.PmsSdkInstance;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
public interface PmsSdkInstanceRepository extends CrudRepository<PmsSdkInstance, String> {
  Optional<PmsSdkInstance> findByName(String name);
  List<PmsSdkInstance> findByActive(boolean active);
}
