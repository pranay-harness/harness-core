package io.harness.resourcegroup.framework.repositories.custom;

import io.harness.resourcegroup.model.ResourceGroup;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
public class ResourceGroupRepositoryCustomImpl implements ResourceGroupRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<ResourceGroup> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<ResourceGroup> resourceGroups = mongoTemplate.find(query, ResourceGroup.class);
    return PageableExecutionUtils.getPage(
        resourceGroups, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), ResourceGroup.class));
  }

  @Override
  public Optional<ResourceGroup> find(Criteria criteria) {
    Query query = new Query(criteria);
    return Optional.ofNullable(mongoTemplate.findOne(query, ResourceGroup.class));
  }

  @Override
  public boolean delete(Criteria criteria) {
    Query query = new Query(criteria);
    DeleteResult removeResult = mongoTemplate.remove(query, ResourceGroup.class);
    return removeResult.wasAcknowledged();
  }

  @Override
  public boolean updateMultiple(Query query, Update update) {
    return mongoTemplate.updateMulti(query, update, ResourceGroup.class).wasAcknowledged();
  }
}
