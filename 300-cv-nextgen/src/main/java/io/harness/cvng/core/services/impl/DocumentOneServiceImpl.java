package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.services.api.DocumentOneService;
import io.harness.repositories.DocumentOne;
import io.harness.repositories.repo.DocumentOneRepo;

import com.google.inject.Inject;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class DocumentOneServiceImpl implements DocumentOneService {
  @Inject DocumentOneRepo documentOneRepo;

  @Override
  @Transactional
  public void save(DocumentOne documentOne) {
    DocumentOne one = documentOneRepo.save(documentOne);
    if (one.getAmount() > 5000) {
      throw new RuntimeException("amount cannot be more than 5000");
    }
  }

  @Override
  public List<DocumentOne> get() {
    return (List<DocumentOne>) documentOneRepo.findAll();
  }
}
