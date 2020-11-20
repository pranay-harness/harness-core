package io.harness.connector.repositories.base;

import io.harness.connector.entities.Connector;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface ConnectorCustomRepository { Page<Connector> findAll(Criteria criteria, Pageable pageable); }
