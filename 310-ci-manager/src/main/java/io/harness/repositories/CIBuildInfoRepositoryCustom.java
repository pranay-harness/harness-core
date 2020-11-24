package io.harness.repositories;

import io.harness.ci.beans.entities.CIBuild;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface CIBuildInfoRepositoryCustom {
  Page<CIBuild> findAll(Criteria criteria, Pageable pageable);
  Optional<CIBuild> getBuildById(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, Long buildIdentifier);
  Page<CIBuild> getBuilds(Criteria criteria, Pageable pageable);
}
