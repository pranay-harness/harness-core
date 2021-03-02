package io.harness.cvng.core.repo;

import io.harness.annotation.HarnessRepo;
import io.harness.cvng.core.entities.DocumentOne;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
public interface DocumentOneRepo extends CrudRepository<DocumentOne, String> {}
