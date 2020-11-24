package io.harness.repositories;

import io.harness.ci.beans.entities.CIBuild;

import java.util.Optional;

public interface BuildDao {
  Optional<CIBuild> findByKey(String key);
}
