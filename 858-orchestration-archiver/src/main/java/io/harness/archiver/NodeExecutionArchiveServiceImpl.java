package io.harness.archiver;

import com.google.inject.Inject;
import io.harness.archiver.beans.NodeExecutionArchive;
import io.harness.repositories.NodeExecutionArchiveRepository;

import java.util.List;

public class NodeExecutionArchiveServiceImpl implements NodeExecutionArchiveService{

  @Inject private NodeExecutionArchiveRepository repository;

  @Override
  public List<NodeExecutionArchive> saveAll(List<NodeExecutionArchive> nodeExecutionArchives) {
    return (List<NodeExecutionArchive>) repository.saveAll(nodeExecutionArchives);

  }
}
