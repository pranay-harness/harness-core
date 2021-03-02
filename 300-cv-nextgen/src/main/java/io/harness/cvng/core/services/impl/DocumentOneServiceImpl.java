package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.entities.DocumentOne;
import io.harness.cvng.core.repo.DocumentOneRepo;
import io.harness.cvng.core.services.api.DocumentOneService;

import com.google.inject.Inject;
import java.util.List;

public class DocumentOneServiceImpl implements DocumentOneService {
  @Inject DocumentOneRepo documentOneRepo;

  @Override
  public void save(DocumentOne documentOne) {
    documentOneRepo.save(documentOne);
  }

  @Override
  public List<DocumentOne> get() {
    return (List<DocumentOne>) documentOneRepo.findAll();
  }
}
