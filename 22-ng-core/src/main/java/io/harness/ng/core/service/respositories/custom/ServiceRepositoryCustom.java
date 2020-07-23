package io.harness.ng.core.service.respositories.custom;

import io.harness.ng.core.service.entity.ServiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface ServiceRepositoryCustom { Page<ServiceEntity> findAll(Criteria criteria, Pageable pageable); }
