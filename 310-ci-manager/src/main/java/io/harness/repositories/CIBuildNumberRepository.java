/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.ci.beans.entities.BuildNumberDetails;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface CIBuildNumberRepository
    extends PagingAndSortingRepository<BuildNumberDetails, String>, CIBuildNumberRepositoryCustom {}
