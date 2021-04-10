package io.harness.gitsync.persistance.testing;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitSyncableEntity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@OwnedBy(DX)
public class NoOpGitAwarePersistenceImpl implements GitAwarePersistence {
  @Inject MongoTemplate mongoTemplate;

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> B save(
      B objectToSave, Y yaml, ChangeType changeType, Class<B> entityClass) {
    return mongoTemplate.save(objectToSave);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> B save(B objectToSave, Y yaml, Class<B> entityClass) {
    return mongoTemplate.save(objectToSave);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> B save(
      B objectToSave, ChangeType changeType, Class<B> entityClass) {
    return mongoTemplate.save(objectToSave);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> Long count(@NotNull Criteria criteria, Pageable pageable,
      String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    Query query = new Query(criteria).with(pageable);
    return mongoTemplate.count(query, entityClass);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> Optional<B> findOne(@NotNull Criteria criteria,
      String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    final List<B> list = find(criteria, null, projectIdentifier, orgIdentifier, accountId, entityClass);
    return Optional.ofNullable(list).map(l -> isEmpty(l) ? null : l.get(0));
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> List<B> find(@NotNull Criteria criteria, Pageable pageable,
      String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    Query query = new Query(criteria).with(pageable);
    return mongoTemplate.find(query, entityClass);
  }

  /**
   * finds and modify first.
   */
  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> B findAndModify(Criteria criteria, Update update,
      ChangeType changeType, String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    Query query = new Query(criteria);
    return mongoTemplate.findAndModify(query, update, entityClass);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> boolean exists(@NotNull Criteria criteria,
      String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    Query query = new Query(criteria);
    return mongoTemplate.exists(query, entityClass);
  }
}
