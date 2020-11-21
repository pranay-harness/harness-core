package io.harness.app.dao.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.ci.beans.entities.BuildNumberDetails;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface CIBuildNumberRepository
    extends PagingAndSortingRepository<BuildNumberDetails, String>, CIBuildNumberRepositoryCustom {}
