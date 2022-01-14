package io.harness.repositories;

import io.harness.ci.beans.entities.BuildNumberDetails;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class CIBuildNumberRepositoryCustomImpl implements CIBuildNumberRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public BuildNumberDetails increaseBuildNumber(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return null;
  }
}
