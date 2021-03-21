package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.Connector;

import java.util.Optional;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@NoRepositoryBean
@OwnedBy(DX)
public interface ConnectorBaseRepository<T extends Connector> extends PagingAndSortingRepository<T, String> {
  Optional<T> findByFullyQualifiedIdentifierAndDeletedNot(String fullyQualifiedIdentifier, boolean notDeleted);
  boolean existsByFullyQualifiedIdentifier(String fullyQualifiedIdentifier);
}
