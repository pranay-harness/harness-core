/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
