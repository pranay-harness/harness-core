package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.beans.SampleBean;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@HarnessRepo
public interface TestRepository extends CrudRepository<SampleBean, String>, TestCustomRepository {}
