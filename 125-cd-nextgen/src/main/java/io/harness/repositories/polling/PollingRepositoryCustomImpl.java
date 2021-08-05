package io.harness.repositories.polling;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.bean.PollingDocument;
import io.harness.polling.bean.PollingDocument.PollingDocumentKeys;
import io.harness.polling.bean.PollingInfo;

import com.google.inject.Inject;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.CDC)
public class PollingRepositoryCustomImpl implements PollingRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public UpdateResult updatePollingInfo(PollingInfo pollingInfo, String pollDocId) {
    Query query = new Query().addCriteria(Criteria.where(PollingDocumentKeys.uuid).is(pollDocId));
    Update update = new Update().set(PollingDocumentKeys.pollingInfo, pollingInfo);
    return mongoTemplate.updateFirst(query, update, PollingDocument.class);
  }

  @Override
  public PollingDocument addSubscribersToExistingPollingDoc(
      String accountId, String orgId, String projectId, PollingInfo pollingInfo, List<String> signatures) {
    Query query = getQuery(accountId, orgId, projectId, pollingInfo);
    Update update = new Update().addToSet(PollingDocumentKeys.signature).each(signatures);
    return mongoTemplate.findAndModify(query, update, PollingDocument.class);
  }

  public Query getQuery(String accountId, String orgId, String projectId, PollingInfo pollingInfo) {
    return new Query().addCriteria(new Criteria()
                                       .and(PollingDocumentKeys.accountId)
                                       .is(accountId)
                                       .and(PollingDocumentKeys.orgIdentifier)
                                       .is(orgId)
                                       .and(PollingDocumentKeys.projectIdentifier)
                                       .is(projectId)
                                       .and(PollingDocumentKeys.pollingInfo)
                                       .is(pollingInfo));
  }

  @Override
  public PollingDocument deleteDocumentIfOnlySubscriber(
      String accountId, String orgId, String projectId, PollingInfo pollingInfo, List<String> signatures) {
    Query query = new Query().addCriteria(new Criteria()
                                              .and(PollingDocumentKeys.accountId)
                                              .is(accountId)
                                              .and(PollingDocumentKeys.orgIdentifier)
                                              .is(orgId)
                                              .and(PollingDocumentKeys.projectIdentifier)
                                              .is(projectId)
                                              .and(PollingDocumentKeys.signature)
                                              .is(signatures)
                                              .and(PollingDocumentKeys.pollingInfo)
                                              .is(pollingInfo));
    return mongoTemplate.findAndRemove(query, PollingDocument.class);
  }

  @Override
  public PollingDocument deleteSubscribersFromExistingPollingDoc(
      String accountId, String orgId, String projectId, PollingInfo pollingInfo, List<String> signatures) {
    Object[] signatureList = signatures.toArray();
    Query query = getQuery(accountId, orgId, projectId, pollingInfo);
    Update update = new Update().pullAll(PollingDocumentKeys.signature, signatureList);
    return mongoTemplate.findAndModify(query, update, PollingDocument.class);
  }

  @Override
  public PollingDocument findPollingDocBySignature(String accountId, List<String> signatures) {
    Query query = new Query().addCriteria(new Criteria()
                                              .and(PollingDocumentKeys.accountId)
                                              .is(accountId)
                                              .and(PollingDocumentKeys.signature)
                                              .in(signatures));
    return (PollingDocument) mongoTemplate.find(query, PollingDocument.class);
  }

  @Override
  public UpdateResult updateSelectiveEntity(String accountId, String pollDocId, String key, Object value) {
    Query query = new Query().addCriteria(
        new Criteria().and(PollingDocumentKeys.accountId).is(accountId).and(PollingDocumentKeys.uuid).is(pollDocId));
    Update update = new Update().set(key, value);
    return mongoTemplate.updateFirst(query, update, PollingDocument.class);
  }
}
