/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.beans.YamlDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.SimpleMongoRepository;
import org.springframework.util.Assert;

@Singleton
//@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitAwareRepositoryImpl<T extends GitSyncableEntity, Y extends YamlDTO>
    extends SimpleMongoRepository<T, String> implements GitAwareRepository<T, Y> {
  @Inject private GitAwarePersistence gitAwarePersistence;
  private MongoEntityInformation<T, String> mongoEntityInformation;

  public GitAwareRepositoryImpl(MongoEntityInformation<T, String> metadata, MongoOperations mongoOperations) {
    super(metadata, mongoOperations);
    this.mongoEntityInformation = metadata;
  }

  @Override
  public T save(T entity, Y yaml) {
    Assert.notNull(entity, "Entity must not be null!");
    //    return gitAwarePersistence.save(entity, yaml, ChangeType.ADD, mongoEntityInformation.getJavaType());
    return null;
  }

  @Override
  public T save(T entity, Y yaml, ChangeType changeType) {
    Assert.notNull(entity, "Entity must not be null!");
    //    return gitAwarePersistence.save(entity, yaml, changeType, mongoEntityInformation.getJavaType());
    return null;
  }

  @Override
  public T save(T entity, ChangeType changeType) {
    Assert.notNull(entity, "Entity must not be null!");
    return gitAwarePersistence.save(entity, changeType, mongoEntityInformation.getJavaType());
  }
}
