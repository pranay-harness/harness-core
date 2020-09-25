package io.harness.ng.core.entityReference.service;

import io.harness.ng.core.entityReference.dto.EntityReferenceDTO;
import org.springframework.data.domain.Page;

public interface EntityReferenceService {
  Page<EntityReferenceDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String referredEntityIdentifier, String searchTerm);

  EntityReferenceDTO save(EntityReferenceDTO entityReferenceDTO);

  Boolean delete(String referredEntityFQN, String referredByEntityFQN);

  Boolean isEntityReferenced(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
