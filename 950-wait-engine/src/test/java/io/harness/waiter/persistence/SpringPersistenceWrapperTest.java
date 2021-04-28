package io.harness.waiter.persistence;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.waiter.TestNotifyEventListener.TEST_PUBLISHER;
import static io.harness.waiter.WaitInstanceService.MAX_CALLBACK_PROCESSING_TIME;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.WaitEngineTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import io.harness.waiter.NotifyResponse;
import io.harness.waiter.TestNotifyCallback;
import io.harness.waiter.TestProgressCallback;
import io.harness.waiter.WaitInstance;
import io.harness.waiter.WaitInstance.WaitInstanceKeys;

import com.google.inject.Inject;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(HarnessTeam.PIPELINE)
public class SpringPersistenceWrapperTest extends WaitEngineTestBase {
  @Inject SpringPersistenceWrapper persistenceWrapper;
  @Inject MongoTemplate mongoTemplate;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  @RealMongo
  public void testSave() {
    String waitInstanceId = generateUuid();
    String correlationId = generateUuid();
    final WaitInstance waitInstance = WaitInstance.builder()
                                          .uuid(waitInstanceId)
                                          .callback(new TestNotifyCallback())
                                          .progressCallback(new TestProgressCallback())
                                          .publisher(TEST_PUBLISHER)
                                          .correlationIds(Collections.singletonList(correlationId))
                                          .waitingOnCorrelationIds(Collections.singletonList(correlationId))
                                          .build();

    String savedInstanceId = persistenceWrapper.save(waitInstance);
    assertThat(savedInstanceId).isEqualTo(waitInstanceId);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  @RealMongo
  public void testFetchForProcessingWaitInstance() {
    String waitInstanceId = generateUuid();
    String correlationId = generateUuid();
    final WaitInstance waitInstance = WaitInstance.builder()
                                          .uuid(waitInstanceId)
                                          .callback(new TestNotifyCallback())
                                          .progressCallback(new TestProgressCallback())
                                          .publisher(TEST_PUBLISHER)
                                          .correlationIds(Collections.singletonList(correlationId))
                                          .waitingOnCorrelationIds(Collections.singletonList(correlationId))
                                          .build();
    mongoTemplate.save(waitInstance);

    long now = System.currentTimeMillis();
    persistenceWrapper.fetchForProcessingWaitInstance(waitInstanceId, now);
    WaitInstance processedWaitInstance =
        mongoTemplate.findOne(query(where(WaitInstanceKeys.uuid).is(waitInstanceId)), WaitInstance.class);
    assertThat(processedWaitInstance.getCallbackProcessingAt())
        .isEqualTo(now + MAX_CALLBACK_PROCESSING_TIME.toMillis());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  @RealMongo
  public void testModifyAndFetchWaitInstance() {
    String waitInstanceId = generateUuid();
    String correlationId = generateUuid();
    final WaitInstance waitInstance = WaitInstance.builder()
                                          .uuid(waitInstanceId)
                                          .callback(new TestNotifyCallback())
                                          .progressCallback(new TestProgressCallback())
                                          .publisher(TEST_PUBLISHER)
                                          .correlationIds(Collections.singletonList(correlationId))
                                          .waitingOnCorrelationIds(Collections.singletonList(correlationId))
                                          .build();
    mongoTemplate.save(waitInstance);
    WaitInstance modifiedWaitInstance = persistenceWrapper.modifyAndFetchWaitInstance(correlationId);
    assertThat(modifiedWaitInstance.getCorrelationIds()).containsExactly(correlationId);
    assertThat(modifiedWaitInstance.getWaitingOnCorrelationIds()).isEmpty();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  @RealMongo
  public void testModifyAndFetchWaitInstanceForNoExistingResponse() {
    String waitInstanceId = generateUuid();
    String correlationId = generateUuid();
    final WaitInstance waitInstance = WaitInstance.builder()
                                          .uuid(waitInstanceId)
                                          .callback(new TestNotifyCallback())
                                          .progressCallback(new TestProgressCallback())
                                          .publisher(TEST_PUBLISHER)
                                          .correlationIds(Collections.singletonList(correlationId))
                                          .waitingOnCorrelationIds(Collections.singletonList(correlationId))
                                          .build();
    mongoTemplate.save(waitInstance);
    WaitInstance modifiedWaitInstance = persistenceWrapper.modifyAndFetchWaitInstanceForExistingResponse(
        waitInstanceId, Collections.singletonList(correlationId));
    assertThat(modifiedWaitInstance).isNull();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  @RealMongo
  public void testModifyAndFetchWaitInstanceForExistingResponse() {
    String waitInstanceId = generateUuid();
    String correlationId = generateUuid();
    final WaitInstance waitInstance = WaitInstance.builder()
                                          .uuid(waitInstanceId)
                                          .callback(new TestNotifyCallback())
                                          .progressCallback(new TestProgressCallback())
                                          .publisher(TEST_PUBLISHER)
                                          .correlationIds(Collections.singletonList(correlationId))
                                          .waitingOnCorrelationIds(Collections.singletonList(correlationId))
                                          .build();
    mongoTemplate.save(waitInstance);

    final NotifyResponse response =
        NotifyResponse.builder().uuid(correlationId).responseData(new byte[] {}).error(false).build();
    mongoTemplate.save(response);

    WaitInstance modifiedWaitInstance = persistenceWrapper.modifyAndFetchWaitInstanceForExistingResponse(
        waitInstanceId, Collections.singletonList(correlationId));
    assertThat(modifiedWaitInstance).isNotNull();
    assertThat(modifiedWaitInstance.getWaitingOnCorrelationIds()).isEmpty();
    assertThat(modifiedWaitInstance.getCorrelationIds()).containsExactly(correlationId);
  }
}