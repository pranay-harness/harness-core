package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.beans.SampleBean;

import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@HarnessRepo
public interface TestRepository extends Repository<SampleBean, String>, TestCustomRepository {}
