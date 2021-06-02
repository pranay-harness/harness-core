package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.entities.Connector;
import io.harness.git.model.ChangeType;

import com.mongodb.client.result.UpdateResult;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

//@NoRepositoryBean
@OwnedBy(DX)
public interface ConnectorCustomRepository {
  // todo(abhinav): This method is not yet migrated because of find By fqn
  Page<Connector> findAll(Criteria criteria, Pageable pageable, boolean getDistinctIdentifiers);

  Page<Connector> findAll(
      Criteria criteria, Pageable pageable, String projectIdentifier, String orgIdentifier, String accountIdentifier);

  Connector update(Criteria criteria, Update update, ChangeType changeType, String projectIdentifier,
      String orgIdentifier, String accountIdentifier);

  UpdateResult updateMultiple(Query query, Update update);

  <T> AggregationResults<T> aggregate(Aggregation aggregation, Class<T> classToFillResultIn);

  Optional<Connector> findByFullyQualifiedIdentifierAndDeletedNot(String fullyQualifiedIdentifier,
      String projectIdentifier, String orgIdentifier, String accountIdentifier, boolean notDeleted);

  boolean existsByFullyQualifiedIdentifier(
      String fullyQualifiedIdentifier, String projectIdentifier, String orgIdentifier, String accountId);

  Connector save(Connector objectToSave, ConnectorDTO yaml);

  Connector save(Connector objectToSave, ChangeType changeType);

  Connector save(Connector objectToSave, ConnectorDTO connectorDTO, ChangeType changeType);
}
