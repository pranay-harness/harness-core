package io.harness.archiver;

import io.harness.archiver.beans.NodeExecutionArchive;

import java.util.List;

public interface NodeExecutionArchiveService{
  List<NodeExecutionArchive> saveAll(List<NodeExecutionArchive> nodeExecutionArchives);
}
