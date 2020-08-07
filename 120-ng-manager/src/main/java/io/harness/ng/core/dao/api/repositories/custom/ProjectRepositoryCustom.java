package io.harness.ng.core.dao.api.repositories.custom;

import io.harness.ng.core.entities.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface ProjectRepositoryCustom { Page<Project> findAll(Criteria criteria, Pageable pageable); }
