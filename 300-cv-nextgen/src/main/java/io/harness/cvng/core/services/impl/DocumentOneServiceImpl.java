package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.services.api.DocumentOneService;
import io.harness.repositories.DocumentOne;
import io.harness.repositories.repo.DocumentOneRepo;

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
