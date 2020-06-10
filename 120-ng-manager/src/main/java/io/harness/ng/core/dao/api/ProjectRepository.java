package io.harness.ng.core.dao.api;

import io.harness.ng.core.entities.Project;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ProjectRepository extends PagingAndSortingRepository<Project, String> {}
