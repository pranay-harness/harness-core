package io.harness.ng.core.api.repositories.custom;

import io.harness.ng.core.models.Invite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface InviteRepositoryCustom { Page<Invite> findAll(Criteria criteria, Pageable pageable); }
