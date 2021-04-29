package io.harness.waiter.persistence;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.waiter.WaitInstanceService.MAX_CALLBACK_PROCESSING_TIME;

import static java.util.stream.Collectors.toList;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;
import io.harness.serializer.KryoSerializer;
import io.harness.springdata.SpringDataMongoUtils;
import io.harness.tasks.ResponseData;
import io.harness.waiter.NotifyResponse;
import io.harness.waiter.NotifyResponse.NotifyResponseKeys;
import io.harness.waiter.ProcessedMessageResponse;
import io.harness.waiter.ProgressUpdate;
import io.harness.waiter.ProgressUpdate.ProgressUpdateKeys;
import io.harness.waiter.WaitEngineEntity;
import io.harness.waiter.WaitInstance;
import io.harness.waiter.WaitInstance.WaitInstanceKeys;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class SpringPersistenceWrapper implements PersistenceWrapper {
  @Inject private MongoTemplate mongoTemplate;
  @Inject private KryoSerializer kryoSerializer;

  private FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().returnNew(false).upsert(false);

  @Override
  public void delete(WaitEngineEntity entity) {
    DeleteResult remove = mongoTemplate.remove(entity);
    if (!remove.wasAcknowledged() || remove.getDeletedCount() != 1) {
      throw new GeneralException("Not able to Delete wait instance");
    }
  }

  @Override
  public ProcessedMessageResponse processMessage(WaitInstance waitInstance) {
    boolean isError = false;
    Map<String, ResponseData> responseMap = new HashMap<>();

    Query query = query(where(NotifyResponseKeys.uuid).in(waitInstance.getCorrelationIds()));
    if (waitInstance.getProgressCallback() != null) {
      query.with(Sort.by(Direction.ASC, NotifyResponseKeys.createdAt));
    }

    List<NotifyResponse> notifyResponses = mongoTemplate.find(query, NotifyResponse.class);
    for (NotifyResponse notifyResponse : notifyResponses) {
      if (notifyResponse.isError()) {
        log.info("Failed notification response {}", notifyResponse.getUuid());
        isError = true;
      }
      if (notifyResponse.getResponseData() != null) {
        responseMap.put(
            notifyResponse.getUuid(), (ResponseData) kryoSerializer.asInflatedObject(notifyResponse.getResponseData()));
      }
    }
    return ProcessedMessageResponse.builder().isError(isError).responseDataMap(responseMap).build();
  }

  @Override
  public String save(WaitEngineEntity entity) {
    WaitEngineEntity savedEntity = mongoTemplate.insert(entity);
    return savedEntity.getUuid();
  }

  @Override
  public WaitInstance modifyAndFetchWaitInstance(String waitingOnCorrelationId) {
    Query query = query(where(WaitInstanceKeys.waitingOnCorrelationIds).in(waitingOnCorrelationId));
    Update update = new Update().pull(WaitInstanceKeys.waitingOnCorrelationIds, waitingOnCorrelationId);
    return mongoTemplate.findAndModify(query, update, SpringDataMongoUtils.returnNewOptions, WaitInstance.class);
  }

  @Override
  public WaitInstance fetchForProcessingWaitInstance(String waitInstanceId, long now) {
    final Query query = query(where(WaitInstanceKeys.uuid).is(waitInstanceId))
                            .addCriteria(where(WaitInstanceKeys.callbackProcessingAt).lt(now));
    final Update update =
        new Update().set(WaitInstanceKeys.callbackProcessingAt, now + MAX_CALLBACK_PROCESSING_TIME.toMillis());
    return mongoTemplate.findAndModify(query, update, findAndModifyOptions, WaitInstance.class);
  }

  @Override
  public ProgressUpdate fetchForProcessingProgressUpdate(Set<String> busyCorrelationIds, long now) {
    Query query;
    if (busyCorrelationIds.isEmpty()) {
      query = new Query().with(Sort.by(ProgressUpdateKeys.createdAt));
    } else {
      query = query(where(ProgressUpdateKeys.correlationId).not().in(busyCorrelationIds))
                  .with(Sort.by(ProgressUpdateKeys.createdAt));
    }
    Update update =
        new Update().set(ProgressUpdateKeys.expireProcessing, now + MAX_CALLBACK_PROCESSING_TIME.toMillis());
    return mongoTemplate.findAndModify(query, update, findAndModifyOptions, ProgressUpdate.class);
  }

  @Override
  public WaitInstance modifyAndFetchWaitInstanceForExistingResponse(
      String waitInstanceId, List<String> correlationIds) {
    // We cannot combine the logic of obtaining the responses before the save, because this will create a race with
    // storing the responses.
    Query query = query(where(NotifyResponseKeys.uuid).in(correlationIds));
    List<NotifyResponse> notifyResponses = mongoTemplate.find(query, NotifyResponse.class);
    final List<String> keys = notifyResponses.stream().map(NotifyResponse::getUuid).collect(toList());
    if (isEmpty(keys)) {
      return null;
    }

    final Query wiQuery = query(where(WaitInstanceKeys.uuid).is(waitInstanceId));
    final Update wiUpdate = new Update().pullAll(WaitInstanceKeys.waitingOnCorrelationIds, keys.toArray(new String[0]));
    return mongoTemplate.findAndModify(wiQuery, wiUpdate, SpringDataMongoUtils.returnNewOptions, WaitInstance.class);
  }
}
