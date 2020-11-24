package io.harness.core.ci.services;

import io.harness.ci.beans.entities.CIBuild;
import io.harness.repositories.CIBuildRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

/**
 * Saves execution of each build to maintain execution history
 */

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class CIBuildServiceImpl implements CIBuildService {
  private final CIBuildRepository ciBuildRepository;

  @Override
  public CIBuild save(CIBuild ciBuild) {
    return ciBuildRepository.save(ciBuild);
  }
}
