package io.harness.gitsync.persistance;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
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

  @Nullable
  public <B> B findById(Object id, Class<B> entityClass) {
    log.info("calling find by id");
    return super.findById(id, entityClass);
  }

  @Nullable
  public <B> B findAndModify(Query query, Update update, Class<B> entityClass) {
    return null;
  }

  @Nullable
  public <B> B findOne(Query query, Class<B> entityClass) {
    return super.findOne(query, entityClass);
  }

  public <B> List<B> find(Query query, Class<B> entityClass) {
    return super.find(query, entityClass);
  }

  public <B> List<B> findDistinct(Query query, String field, Class<?> entityClass, Class<B> resultClass) {
    return super.findDistinct(query, field, entityClass, resultClass);
  }

  public UpdateResult upsert(Query query, Update update, Class<?> entityClass) {
    return super.upsert(query, update, entityClass);
  }
  public UpdateResult updateFirst(Query query, Update update, Class<?> entityClass) {
    return super.updateFirst(query, update, entityClass);
  }

  public DeleteResult remove(Object object, String collectionName) {
    return super.remove(object, collectionName);
  }
}
