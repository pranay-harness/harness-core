package io.harness.notification.repositories;

import io.harness.notification.entities.Notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface NotificationRepositoryCustom {
  Page<Notification> findAll(Criteria criteria, Pageable pageable);
}
