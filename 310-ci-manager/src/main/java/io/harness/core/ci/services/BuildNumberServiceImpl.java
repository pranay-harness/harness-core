/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.core.ci.services;

import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.repositories.CIBuildNumberRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class BuildNumberServiceImpl implements BuildNumberService {
  private final CIBuildNumberRepository ciBuildNumberRepository;
  public BuildNumberDetails increaseBuildNumber(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return ciBuildNumberRepository.increaseBuildNumber(accountIdentifier, orgIdentifier, projectIdentifier);
  }
}
