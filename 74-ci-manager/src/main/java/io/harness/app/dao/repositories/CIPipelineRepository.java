package io.harness.app.dao.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.beans.CIPipeline;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface CIPipelineRepository
    extends PagingAndSortingRepository<CIPipeline, String>, CIPipelineRepositoryCustom {}
