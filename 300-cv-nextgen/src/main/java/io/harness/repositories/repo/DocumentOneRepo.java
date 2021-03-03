package io.harness.repositories.repo;

import io.harness.annotation.HarnessRepo;
import io.harness.repositories.DocumentOne;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
public interface DocumentOneRepo extends CrudRepository<DocumentOne, String> {}
