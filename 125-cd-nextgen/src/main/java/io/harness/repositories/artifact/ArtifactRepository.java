package io.harness.repositories.artifact;

import io.harness.annotation.HarnessRepo;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface ArtifactRepository extends PagingAndSortingRepository<ArtifactSource, String> {
  ArtifactSource findByAccountIdAndUniqueHash(String accountId, String uniqueHash);
  ArtifactSource findByAccountIdAndUuid(String accountId, String uuid);
}
