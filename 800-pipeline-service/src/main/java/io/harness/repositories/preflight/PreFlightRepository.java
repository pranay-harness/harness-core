package io.harness.repositories.preflight;

import io.harness.annotation.HarnessRepo;
import io.harness.pms.preflight.entity.PreFlightEntity;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface PreFlightRepository
    extends PagingAndSortingRepository<PreFlightEntity, String>, PreFlightRepositoryCustom {}
