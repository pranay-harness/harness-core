package io.harness.core.ci.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.app.dao.repositories.CIBuildNumberRepository;
import io.harness.ci.beans.entities.BuildNumberDetails;
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
