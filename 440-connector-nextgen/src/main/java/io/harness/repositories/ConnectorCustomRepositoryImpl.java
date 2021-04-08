package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.connector.entities.Connector.CONNECTOR_COLLECTION_NAME;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.Connector.ConnectorKeys;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitSyncableHarnessRepo;

import com.google.inject.Inject;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@GitSyncableHarnessRepo
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class ConnectorCustomRepositoryImpl implements ConnectorCustomRepository {
  private MongoTemplate mongoTemplate;
  private GitAwarePersistence gitAwarePersistence;

  // todo(abhinav): This method is not yet migrated because of find By fqn
  @Override
  public Page<Connector> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<Connector> connectors = mongoTemplate.find(query, Connector.class);
    return PageableExecutionUtils.getPage(
        connectors, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Connector.class));
  }

  @Override
  public Page<Connector> findAll(
      Criteria criteria, Pageable pageable, String projectIdentifier, String orgIdentifier, String accountIdentifier) {
    Query query = new Query(criteria).with(pageable);
    List<Connector> connectors =
        gitAwarePersistence.find(query, projectIdentifier, orgIdentifier, accountIdentifier, Connector.class);
    return PageableExecutionUtils.getPage(connectors, pageable,
        ()
            -> gitAwarePersistence.count(Query.of(query).limit(-1).skip(-1), projectIdentifier, orgIdentifier,
                accountIdentifier, Connector.class));
  }

  @Override
  public Connector update(Query query, Update update, ChangeType changeType, String projectIdentifier,
      String orgIdentifier, String accountIdentifier) {
    return gitAwarePersistence.findAndModify(
        query, update, changeType, projectIdentifier, orgIdentifier, accountIdentifier, Connector.class);
  }

  @Override
  public UpdateResult updateMultiple(Query query, Update update) {
    return mongoTemplate.updateMulti(query, update, Connector.class);
  }

  @Override
  public <T> AggregationResults<T> aggregate(Aggregation aggregation, Class<T> classToFillResultIn) {
    return mongoTemplate.aggregate(aggregation, CONNECTOR_COLLECTION_NAME, classToFillResultIn);
  }

  @Override
  public Optional<Connector> findByFullyQualifiedIdentifierAndDeletedNot(String fullyQualifiedIdentifier,
      String projectIdentifier, String orgIdentifier, String accountIdentifier, boolean notDeleted) {
    return Optional
        .ofNullable(gitAwarePersistence.find(query(Criteria.where(ConnectorKeys.fullyQualifiedIdentifier)
                                                       .is(fullyQualifiedIdentifier)
                                                       .and(ConnectorKeys.deleted)
                                                       .is(!notDeleted))
                                                 .limit(1),
            projectIdentifier, orgIdentifier, accountIdentifier, Connector.class))
        .map(l -> isEmpty(l) ? null : l.get(0));
  }

  @Override
  public boolean existsByFullyQualifiedIdentifier(
      String fullyQualifiedIdentifier, String projectIdentifier, String orgIdentifier, String accountId) {
    return gitAwarePersistence.exists(
        query(Criteria.where(ConnectorKeys.fullyQualifiedIdentifier).is(fullyQualifiedIdentifier)), projectIdentifier,
        orgIdentifier, accountId, Connector.class);
  }

  @Override
  public Connector save(Connector objectToSave, ConnectorDTO yaml) {
    return gitAwarePersistence.save(objectToSave, yaml, Connector.class);
  }

  @Override
  public Connector save(Connector objectToSave, ChangeType changeType) {
    return gitAwarePersistence.save(objectToSave, changeType, Connector.class);
  }

  @Override
  public Connector save(Connector objectToSave, ConnectorDTO connectorDTO, ChangeType changeType) {
    return gitAwarePersistence.save(objectToSave, connectorDTO, changeType, Connector.class);
  }
}