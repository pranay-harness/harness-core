package io.harness.gitsync.persistance;

import io.harness.gitsync.interceptor.GitBranchInfo;
import io.harness.gitsync.interceptor.GitSyncBranchThreadLocal;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.lang.Nullable;

@Slf4j
//@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject}))
public class GitAwarePersistence extends MongoTemplate {
  public GitAwarePersistence(MongoDbFactory mongoDbFactory, MongoConverter mongoConverter) {
    super(mongoDbFactory, mongoConverter);
  }

  // todo(abhinav): add all and support them one by one.
  @Nullable
  public <B> B findById(@NotNull Object id, @NotNull Class<B> entityClass) {
    log.info("calling find by id");
    return super.findById(id, entityClass);
  }

  @Nullable
  public <B> B findAndModify(@NotNull Query query, @NotNull Update update, @NotNull Class<B> entityClass) {
    return null;
  }

  @Nullable
  public <B> B findOne(@NotNull Query query, @NotNull Class<B> entityClass) {
    return super.findOne(query, entityClass);
  }

  public <B> List<B> find(@NotNull Query query, @NotNull Class<B> entityClass) {
    return super.find(query, entityClass);
  }

  public <B> List<B> findDistinct(
      @NotNull Query query, @NotNull String field, @NotNull Class<?> entityClass, @NotNull Class<B> resultClass) {
    return super.findDistinct(query, field, entityClass, resultClass);
  }

  public UpdateResult upsert(@NotNull Query query, @NotNull Update update, @NotNull Class<?> entityClass) {
    return super.upsert(query, update, entityClass);
  }

  public UpdateResult updateFirst(@NotNull Query query, @NotNull Update update, @NotNull Class<?> entityClass) {
    return super.updateFirst(query, update, entityClass);
  }

  public DeleteResult remove(@NotNull Object object, @NotNull String collectionName) {
    return super.remove(object, collectionName);
  }

  public <T> T save(T objectToSave) {
    getAndSetBranchInfo(objectToSave);
    return super.save(objectToSave);
  }

  public <T> T insert(T objectToSave) {
    getAndSetBranchInfo(objectToSave);
    return super.insert(objectToSave);
  }

  public <T> T insert(T objectToSave, String collectionName) {
    getAndSetBranchInfo(objectToSave);
    return super.insert(objectToSave, collectionName);
  }

  private <T> void getAndSetBranchInfo(T objectToSave) {
    if (objectToSave instanceof GitSyncableEntity) {
      final GitBranchInfo gitBranchInfo = GitSyncBranchThreadLocal.get();
      final String branch = gitBranchInfo.getBranch();
      ((GitSyncableEntity) objectToSave).setBranch(branch);
    }
  }
}
