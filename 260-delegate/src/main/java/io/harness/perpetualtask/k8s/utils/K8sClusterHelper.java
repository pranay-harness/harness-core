package io.harness.perpetualtask.k8s.utils;

import io.harness.filesystem.FileIo;

import com.google.common.collect.Sets;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Singleton
@Service
public class K8sClusterHelper {
  public static final String SEEN_CLUSTER_FILE_NAME = "k8sSeenClusters.txt";
  private static final String DELIMITER = ",";

  private static Set<String> seenClusterList;

  static {
    try {
      seenClusterList = loadFile();
    } catch (Exception ex) {
      log.error("Error initializing seenClusterList, loadFile(). Harmless error.", ex);
      seenClusterList = new HashSet<>();
    }
  }

  // Practically list of clusterIds shouldn't be much large, else we can 1) delete file after months/year or, 2) have
  // timestamp with each clusterId and evict only those which hasn't been accessed in a long time months/year

  private static void updateFile() throws IOException {
    log.info("updateFile {}", SEEN_CLUSTER_FILE_NAME);
    String toWrite = String.join(DELIMITER, seenClusterList);
    FileIo.writeWithExclusiveLockAcrossProcesses(toWrite, SEEN_CLUSTER_FILE_NAME, StandardOpenOption.TRUNCATE_EXISTING);
  }

  private static Set<String> loadFile() throws Exception {
    log.info("loadFile {}", SEEN_CLUSTER_FILE_NAME);
    if (FileIo.checkIfFileExist(SEEN_CLUSTER_FILE_NAME)) {
      String fileContent = FileIo.getFileContentsWithSharedLockAcrossProcesses(SEEN_CLUSTER_FILE_NAME).trim();
      log.info("Old Clusters: {}", fileContent);
      return Sets.newHashSet(fileContent.split(DELIMITER));
    }
    log.info("File does not exists, creating one");
    FileIo.writeWithExclusiveLockAcrossProcesses("", SEEN_CLUSTER_FILE_NAME, StandardOpenOption.CREATE);
    return new HashSet<>();
  }

  public static boolean isSeen(@NotNull String clusterInfo) {
    log.info("Cluster {} is seen:{}", clusterInfo, seenClusterList.contains(clusterInfo));
    return seenClusterList.contains(clusterInfo);
  }

  public static void setAsSeen(@NotNull String clusterInfo) {
    seenClusterList.add(clusterInfo);
    try {
      updateFile();
    } catch (IOException ex) {
      log.error("Failed to updateFile {} after setAsSeen", SEEN_CLUSTER_FILE_NAME, ex);
    }
  }

  public static void clean() throws IOException {
    FileIo.deleteFileIfExists(SEEN_CLUSTER_FILE_NAME);
  }
}
