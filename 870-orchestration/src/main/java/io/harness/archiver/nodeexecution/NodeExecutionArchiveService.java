package io.harness.archiver.nodeexecution;

import java.util.List;

public interface NodeExecutionArchiveService{
  List<NodeExecutionArchive> saveAll(List<NodeExecutionArchive> nodeExecutionArchives);
}
