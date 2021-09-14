/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.file.dao;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.FileBucket;
import io.harness.file.GcsHarnessFileMetadata;

import java.util.List;
import java.util.Map;

@OwnedBy(PL)
public interface GcsHarnessFileMetadataDao {
  String getGcsFileIdByMongoFileId(String mongoFileId);

  String getMongoFileIdByGcsFileId(String gcsFileId);

  void deleteGcsFileMetadataByMongoFileId(String mongoFileId);

  boolean updateGcsFileMetadata(String gcsFileId, String entityId, Integer version, Map<String, Object> others);

  GcsHarnessFileMetadata getFileMetadataByGcsFileId(String gcsFileId);

  List<String> getAllGcsFileIdsFromGcsFileMetadata(String entityId, FileBucket fileBucket);

  String getLatestGcsFileIdFromGcsFileMetadata(String entityId, FileBucket fileBucket);

  String getLatestGcsFileIdFromGcsFileMetadataByQualifier(String entityId, FileBucket fileBucket, String qualifier);

  String getGcsFileIdFromGcsFileMetadataByVersion(String entityId, Integer version, FileBucket fileBucket);

  void deleteGcsFileMetadataByGcsFileId(String gcsFileId);

  void save(GcsHarnessFileMetadata gcsHarnessFileMetadata);
}
