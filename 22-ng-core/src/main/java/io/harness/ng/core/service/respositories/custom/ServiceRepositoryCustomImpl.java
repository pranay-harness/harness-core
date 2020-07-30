package io.harness.ng.core.service.respositories.custom;

import com.google.inject.Inject;

import com.mongodb.client.result.UpdateResult;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.mappers.ServiceFilterHelper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ServiceRepositoryCustomImpl implements ServiceRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<ServiceEntity> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<ServiceEntity> projects = mongoTemplate.find(query, ServiceEntity.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), ServiceEntity.class));
  }

  @Override
  public UpdateResult upsert(Criteria criteria, ServiceEntity serviceEntity) {
    Query query = new Query(criteria);
    Update update = ServiceFilterHelper.getUpdateOperations(serviceEntity);
    return mongoTemplate.upsert(query, update, ServiceEntity.class);
  }

  @Override
  public UpdateResult update(Criteria criteria, ServiceEntity serviceEntity) {
    Query query = new Query(criteria);
    Update update = ServiceFilterHelper.getUpdateOperations(serviceEntity);
    return mongoTemplate.updateFirst(query, update, ServiceEntity.class);
  }
}
