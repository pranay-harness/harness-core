package io.harness.repositories;

import static io.harness.connector.entities.Connector.CONNECTOR_COLLECTION_NAME;

import io.harness.connector.ConnectorDTO;
import io.harness.connector.entities.Connector;
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
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@GitSyncableHarnessRepo
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class ConnectorCustomRepositoryImpl implements ConnectorCustomRepository {
  private MongoTemplate mongoTemplate;
  private GitAwarePersistence gitAwarePersistence;

  @Override
  public Page<Connector> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<Connector> connectors = mongoTemplate.find(query, Connector.class);
    return PageableExecutionUtils.getPage(
        connectors, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Connector.class));
  }

  @Override
  public Connector update(Query query, Update update) {
    return mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true), Connector.class);
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
  public Optional<Connector> findByFullyQualifiedIdentifierAndDeletedNot(
      String fullyQualifiedIdentifier, boolean notDeleted) {
    return Optional.empty();
  }

  @Override
  public boolean existsByFullyQualifiedIdentifier(String fullyQualifiedIdentifier) {
    return false;
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