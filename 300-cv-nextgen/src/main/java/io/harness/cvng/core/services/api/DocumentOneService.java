package io.harness.cvng.core.services.api;

import io.harness.cvng.core.entities.DocumentOne;

import java.util.List;

public interface DocumentOneService {
  void save(DocumentOne documentOne);

  List<DocumentOne> get();
}
