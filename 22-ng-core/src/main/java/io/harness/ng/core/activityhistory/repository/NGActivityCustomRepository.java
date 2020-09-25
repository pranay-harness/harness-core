package io.harness.ng.core.activityhistory.repository;

import io.harness.ng.core.activityhistory.entity.NGActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface NGActivityCustomRepository { Page<NGActivity> findAll(Criteria criteria, Pageable pageable); }
