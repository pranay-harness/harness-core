package io.harness.app.dao.repositories;

import io.harness.ci.beans.entities.CIBuild;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

public interface CIBuildInfoRepositoryCustom {
  Page<CIBuild> findAll(Criteria criteria, Pageable pageable);
  Optional<CIBuild> getBuildById(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, Long buildIdentifier);
  Page<CIBuild> getBuilds(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, Criteria criteria, Pageable pageable);
}
