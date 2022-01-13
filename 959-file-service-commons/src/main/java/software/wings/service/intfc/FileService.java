/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.FileMetadata;
import io.harness.file.HarnessFile;
import io.harness.stream.BoundedInputStream;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * The Interface FileService.
 */
@OwnedBy(PL)
@TargetModule(HarnessModule._960_PERSISTENCE)
public interface FileService {
  String saveFile(FileMetadata fileMetadata, InputStream in, FileBucket fileBucket);

  boolean updateParentEntityIdAndVersion(Class entityClass, String entityId, Integer version, String fileId,
      Map<String, Object> others, FileBucket fileBucket);

  String saveFile(HarnessFile baseFile, InputStream uploadedInputStream, FileBucket fileBucket);

  void deleteFile(String fileId, FileBucket fileBucket);

  File download(String fileId, File file, FileBucket fileBucket);

  void downloadToStream(String fileId, OutputStream op, FileBucket fileBucket);

  InputStream openDownloadStream(String fileId, FileBucket fileBucket);

  FileMetadata getFileMetadata(String fileId, FileBucket fileBucket);

  List<String> getAllFileIds(String entityId, FileBucket fileBucket);

  String getLatestFileId(String entityId, FileBucket fileBucket);

  String getLatestFileIdByQualifier(String entityId, FileBucket fileBucket, String qualifier);

  String getFileIdByVersion(String entityId, int version, FileBucket fileBucket);

  String uploadFromStream(String filename, BoundedInputStream in, FileBucket fileBucket, Map<String, Object> metaData);

  void deleteAllFilesForEntity(String entityId, FileBucket fileBucket);
}
