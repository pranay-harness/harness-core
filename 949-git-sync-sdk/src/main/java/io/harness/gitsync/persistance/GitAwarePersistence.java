package io.harness.gitsync.persistance;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public interface GitAwarePersistence {
  <B> B findAndModify(@NotNull Query query, @NotNull Update update, @NotNull Class<B> entityClass);

  <B> B findOne(@NotNull Query query, @NotNull Class<B> entityClass);

  <B> List<B> find(@NotNull Query query, @NotNull Class<B> entityClass);

  <B> List<B> findDistinct(
      @NotNull Query query, @NotNull String field, @NotNull Class<?> entityClass, @NotNull Class<B> resultClass);

  <Y> UpdateResult upsert(@NotNull Query query, @NotNull Update update, @NotNull Class<?> entityClass, Y yaml);

  <Y> UpdateResult updateFirst(@NotNull Query query, @NotNull Update update, @NotNull Class<?> entityClass, Y yaml);

  <Y> DeleteResult remove(@NotNull GitSyncableEntity object, @NotNull String collectionName, Y yaml);

  <Y> DeleteResult remove(@NotNull Object object, Y yaml);

  <T extends GitSyncableEntity, Y> T save(T objectToSave, Y yaml);

  <T extends GitSyncableEntity, Y> T insert(T objectToSave, Y yaml);

  <T extends GitSyncableEntity, Y> T insert(T objectToSave, String collectionName, Y yaml);
}
