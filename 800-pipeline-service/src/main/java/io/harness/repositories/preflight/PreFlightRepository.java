/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.preflight;

import io.harness.annotation.HarnessRepo;
import io.harness.pms.preflight.entity.PreFlightEntity;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface PreFlightRepository
    extends PagingAndSortingRepository<PreFlightEntity, String>, PreFlightRepositoryCustom {}
