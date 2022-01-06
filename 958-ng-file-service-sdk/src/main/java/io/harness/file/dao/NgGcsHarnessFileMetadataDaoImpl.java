/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.file.dao;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.springframework.data.domain.Sort.Direction.DESC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.FileBucket;
import io.harness.file.CommonGcsHarnessFileMetadata;
import io.harness.file.GcsHarnessFileMetadata;
import io.harness.file.entities.NgGcsFileMetadata;
import io.harness.file.entities.NgGcsFileMetadata.NgGcsFileMetadataKeys;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@Slf4j
@OwnedBy(PL)
public class NgGcsHarnessFileMetadataDaoImpl implements GcsHarnessFileMetadataDao {
  private final MongoTemplate mongoTemplate;

  @Inject
  public NgGcsHarnessFileMetadataDaoImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public String getGcsFileIdByMongoFileId(String mongoFileId) {
    Query query = new Query(Criteria.where(NgGcsFileMetadataKeys.fileId).is(mongoFileId));
    NgGcsFileMetadata mapping = mongoTemplate.findOne(query, NgGcsFileMetadata.class);
    return mapping == null ? null : mapping.getGcsFileId();
  }

  @Override
  public String getMongoFileIdByGcsFileId(String gcsFileId) {
    Query query = new Query(Criteria.where(NgGcsFileMetadataKeys.gcsFileId).is(gcsFileId));
    NgGcsFileMetadata mapping = mongoTemplate.findOne(query, NgGcsFileMetadata.class);
    return mapping == null ? null : mapping.getFileId();
  }

  @Override
  public void deleteGcsFileMetadataByMongoFileId(String mongoFileId) {
    Query query = new Query(Criteria.where(NgGcsFileMetadataKeys.fileId).is(mongoFileId));
    NgGcsFileMetadata mapping = mongoTemplate.findOne(query, NgGcsFileMetadata.class);
    if (mapping != null) {
      mongoTemplate.remove(query, NgGcsFileMetadata.class);
    }
  }

  @Override
  public boolean updateGcsFileMetadata(String gcsFileId, String entityId, Integer version, Map<String, Object> others) {
    log.info("Updating GCS file '{}' with parent entity '{}' and version '{}' with {} other metadata entries.",
        gcsFileId, entityId, version, others == null ? 0 : others.size());
    Query query = new Query(Criteria.where(NgGcsFileMetadataKeys.gcsFileId).is(gcsFileId));
    NgGcsFileMetadata gcsFileMetadata = mongoTemplate.findOne(query, NgGcsFileMetadata.class);
    if (gcsFileMetadata == null) {
      log.warn("Can't update GCS file metadata since no corresponding entry is found for file with id '{}'", gcsFileId);
      return false;
    }
    Update update = new Update();
    if (entityId != null) {
      update.set("entityId", entityId);
    }
    if (version != null) {
      update.set("version", version);
    }
    if (isNotEmpty(others)) {
      update.set("others", others);
    }
    UpdateResult result = mongoTemplate.updateFirst(query, update, NgGcsFileMetadata.class);
    return result.getModifiedCount() > 0;
  }

  @Override
  public GcsHarnessFileMetadata getFileMetadataByGcsFileId(String gcsFileId) {
    Query query = new Query(Criteria.where(NgGcsFileMetadataKeys.gcsFileId).is(gcsFileId));
    return mongoTemplate.findOne(query, NgGcsFileMetadata.class);
  }

  @Override
  public List<String> getAllGcsFileIdsFromGcsFileMetadata(String entityId, FileBucket fileBucket) {
    Query query = new Query(Criteria.where(NgGcsFileMetadataKeys.entityId)
                                .is(entityId)
                                .and(NgGcsFileMetadataKeys.fileBucket)
                                .is(fileBucket));
    List<NgGcsFileMetadata> gcsFileMetadatas = mongoTemplate.find(query, NgGcsFileMetadata.class);

    return gcsFileMetadatas.stream().map(NgGcsFileMetadata::getGcsFileId).distinct().collect(Collectors.toList());
  }

  @Override
  public String getLatestGcsFileIdFromGcsFileMetadata(String entityId, FileBucket fileBucket) {
    Query query = new Query(Criteria.where(NgGcsFileMetadataKeys.entityId)
                                .is(entityId)
                                .and(NgGcsFileMetadataKeys.fileBucket)
                                .is(fileBucket))
                      .with(Sort.by(DESC, NgGcsFileMetadataKeys.createdAt));
    NgGcsFileMetadata gcsFileMetadata = mongoTemplate.findOne(query, NgGcsFileMetadata.class);

    return gcsFileMetadata == null ? null : gcsFileMetadata.getGcsFileId();
  }

  @Override
  public String getLatestGcsFileIdFromGcsFileMetadataByQualifier(
      String entityId, FileBucket fileBucket, String qualifier) {
    Query query = new Query(Criteria.where(NgGcsFileMetadataKeys.entityId)
                                .is(entityId)
                                .and(NgGcsFileMetadataKeys.fileBucket)
                                .is(fileBucket)
                                .and("others.qualifier")
                                .is(qualifier))
                      .with(Sort.by(DESC, NgGcsFileMetadataKeys.createdAt));
    NgGcsFileMetadata gcsFileMetadata = mongoTemplate.findOne(query, NgGcsFileMetadata.class);

    return gcsFileMetadata == null ? null : gcsFileMetadata.getGcsFileId();
  }

  @Override
  public String getGcsFileIdFromGcsFileMetadataByVersion(String entityId, Integer version, FileBucket fileBucket) {
    Query query = new Query(Criteria.where(NgGcsFileMetadataKeys.entityId)
                                .is(entityId)
                                .and(NgGcsFileMetadataKeys.fileBucket)
                                .is(fileBucket)
                                .and(NgGcsFileMetadataKeys.version)
                                .is(version));
    NgGcsFileMetadata gcsFileMetadata = mongoTemplate.findOne(query, NgGcsFileMetadata.class);

    return gcsFileMetadata == null ? null : gcsFileMetadata.getGcsFileId();
  }

  @Override
  public void deleteGcsFileMetadataByGcsFileId(String gcsFileId) {
    Query query = new Query(Criteria.where(NgGcsFileMetadataKeys.gcsFileId).is(gcsFileId));
    NgGcsFileMetadata mapping = mongoTemplate.findOne(query, NgGcsFileMetadata.class);
    if (mapping != null) {
      mongoTemplate.remove(query, NgGcsFileMetadata.class);
    }
  }

  @Override
  public void save(GcsHarnessFileMetadata gcsHarnessFileMetadata) {
    CommonGcsHarnessFileMetadata commonGcsHarnessFileMetadata = (CommonGcsHarnessFileMetadata) gcsHarnessFileMetadata;
    NgGcsFileMetadata gcsFileMetadata = NgGcsFileMetadata.builder()
                                            .accountId(commonGcsHarnessFileMetadata.getAccountId())
                                            .fileId(commonGcsHarnessFileMetadata.getFileId())
                                            .gcsFileId(commonGcsHarnessFileMetadata.getGcsFileId())
                                            .fileName(commonGcsHarnessFileMetadata.getFileName())
                                            .fileLength(commonGcsHarnessFileMetadata.getFileLength())
                                            .checksum(commonGcsHarnessFileMetadata.getChecksum())
                                            .checksumType(commonGcsHarnessFileMetadata.getChecksumType())
                                            .mimeType(commonGcsHarnessFileMetadata.getMimeType())
                                            .fileBucket(commonGcsHarnessFileMetadata.getFileBucket())
                                            .build();
    mongoTemplate.save(gcsFileMetadata);
  }
}
