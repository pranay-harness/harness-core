package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretKey;

import org.springframework.data.repository.CrudRepository;

@OwnedBy(PL)
@HarnessRepo
public interface SecretKeyRepository extends CrudRepository<SecretKey, String> {}
