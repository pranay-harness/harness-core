package io.harness.app.dao.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.ci.beans.entities.BuildNumber;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface CIBuildNumberRepository
    extends PagingAndSortingRepository<BuildNumber, String>, CIBuildNumberRepositoryCustom {}
