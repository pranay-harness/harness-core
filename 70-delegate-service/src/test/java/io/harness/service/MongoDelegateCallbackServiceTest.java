package io.harness.service;

import static io.harness.rule.OwnerRule.MARKO;
import static org.assertj.core.api.Assertions.fail;

import com.google.inject.Inject;

import io.harness.DelegateServiceTest;
import io.harness.callback.DelegateCallback;
import io.harness.callback.MongoDatabase;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.rule.Owner;
import io.harness.service.impl.DelegateCallbackRegistryImpl;
import io.harness.service.intfc.DelegateCallbackService;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MongoDelegateCallbackServiceTest extends DelegateServiceTest {
  @Inject DelegateCallbackRegistryImpl delegateCallbackRegistry;

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  @Ignore("Need to find a way to connect to test mongo db using URI")
  public void testPublishTaskResponse() {
    DelegateCallback delegateCallback = DelegateCallback.newBuilder()
                                            .setMongoDatabase(MongoDatabase.newBuilder()
                                                                  .setConnection("mongodb://localhost:1234/harness")
                                                                  .setCollectionNamePrefix("cx")
                                                                  .build())
                                            .build();
    String driverId = delegateCallbackRegistry.ensureCallback(delegateCallback);
    DelegateCallbackService delegateCallbackService = delegateCallbackRegistry.obtainDelegateCallbackService(driverId);

    try {
      delegateCallbackService.publishTaskResponse(
          "taskId", DelegateTaskResponse.builder().accountId("accountId").build());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}
