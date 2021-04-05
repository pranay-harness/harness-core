package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SampleBean;

import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@HarnessRepo
@OwnedBy(DX)
public interface TestRepository extends Repository<SampleBean, String>, TestCustomRepository {}
