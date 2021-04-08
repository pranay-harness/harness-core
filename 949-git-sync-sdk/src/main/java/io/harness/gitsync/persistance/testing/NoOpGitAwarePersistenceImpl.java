package io.harness.gitsync.persistance.testing;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitSyncableEntity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@OwnedBy(DX)
public class NoOpGitAwarePersistenceImpl implements GitAwarePersistence {
  @Inject MongoTemplate mongoTemplate;

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> Long count(
      Query query, String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    return mongoTemplate.count(query, entityClass);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> List<B> find(
      Query query, String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    return mongoTemplate.find(query, entityClass);
  }

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
  public <B extends GitSyncableEntity, Y extends YamlDTO> boolean exists(
      Query query, String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    return mongoTemplate.exists(query, entityClass);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> B save(
      B objectToSave, ChangeType changeType, Class<B> entityClass) {
    return mongoTemplate.save(objectToSave);
  }

  @Override
  public <B extends GitSyncableEntity, Y extends YamlDTO> B findAndModify(Query query, Update update,
      ChangeType changeType, String projectIdentifier, String orgIdentifier, String accountId, Class<B> entityClass) {
    return mongoTemplate.findAndModify(query, update, entityClass);
  }
}
