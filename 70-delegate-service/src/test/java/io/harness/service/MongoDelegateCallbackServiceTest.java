package io.harness.service;

import static com.mongodb.DBCollection.ID_FIELD_NAME;
import static io.harness.rule.OwnerRule.MARKO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.inject.Inject;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.harness.DelegateServiceTestBase;
import io.harness.callback.DelegateCallback;
import io.harness.callback.MongoDatabase;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateAsyncTaskResponse.DelegateAsyncTaskResponseKeys;
import io.harness.delegate.beans.DelegateSyncTaskResponse.DelegateSyncTaskResponseKeys;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.service.impl.DelegateCallbackRegistryImpl;
import io.harness.service.intfc.DelegateCallbackService;
import io.harness.waiter.StringNotifyResponseData;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.InetSocketAddress;

public class MongoDelegateCallbackServiceTest extends DelegateServiceTestBase {
  @Inject DelegateCallbackRegistryImpl delegateCallbackRegistry;
  @Inject private KryoSerializer kryoSerializer;

  public static MongoServer MONGO_SERVER;

  private static MongoServer startMongoServer() {
    final MongoServer mongoServer = new MongoServer(new MemoryBackend());
    mongoServer.bind("localhost", 0);
    return mongoServer;
  }

  private static void stopMongoServer() {
    if (MONGO_SERVER != null) {
      MONGO_SERVER.shutdownNow();
    }
  }

  private static String getMongoUri() {
    InetSocketAddress serverAddress = MONGO_SERVER.getLocalAddress();
    final ServerAddress addr = new ServerAddress(serverAddress);
    return String.format("mongodb://%s:%s/harness", addr.getHost(), addr.getPort());
  }

  @BeforeClass
  public static void beforeClass() {
    MONGO_SERVER = startMongoServer();
  }

  @AfterClass
  public static void afterClass() {
    stopMongoServer();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testPublishSyncTaskResponse() {
    DelegateCallback delegateCallback =
        DelegateCallback.newBuilder()
            .setMongoDatabase(
                MongoDatabase.newBuilder().setConnection(getMongoUri()).setCollectionNamePrefix("cx").build())
            .build();
    String driverId = delegateCallbackRegistry.ensureCallback(delegateCallback);
    DelegateCallbackService delegateCallbackService = delegateCallbackRegistry.obtainDelegateCallbackService(driverId);

    try {
      delegateCallbackService.publishSyncTaskResponse(
          "taskId", kryoSerializer.asDeflatedBytes(StringNotifyResponseData.builder().data("OK").build()));
    } catch (Exception e) {
      fail(e.getMessage());
    }

    MongoClient mongoClient = new MongoClient(new MongoClientURI(getMongoUri()));
    MongoCollection<Document> mongoCollection =
        mongoClient.getDatabase("harness").getCollection("cx_delegateSyncTaskResponses");

    assertThat(mongoCollection.countDocuments()).isEqualTo(1);
    assertThat(mongoCollection.find().first().keySet())
        .containsExactlyInAnyOrder(ID_FIELD_NAME, DelegateSyncTaskResponseKeys.responseData);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testPublishAsyncTaskResponse() {
    DelegateCallback delegateCallback =
        DelegateCallback.newBuilder()
            .setMongoDatabase(
                MongoDatabase.newBuilder().setConnection(getMongoUri()).setCollectionNamePrefix("cx").build())
            .build();
    String driverId = delegateCallbackRegistry.ensureCallback(delegateCallback);
    DelegateCallbackService delegateCallbackService = delegateCallbackRegistry.obtainDelegateCallbackService(driverId);

    try {
      delegateCallbackService.publishAsyncTaskResponse(
          "taskId", kryoSerializer.asDeflatedBytes(StringNotifyResponseData.builder().data("OK").build()));
    } catch (Exception e) {
      fail(e.getMessage());
    }

    MongoClient mongoClient = new MongoClient(new MongoClientURI(getMongoUri()));
    MongoCollection<Document> mongoCollection =
        mongoClient.getDatabase("harness").getCollection("cx_delegateAsyncTaskResponses");

    assertThat(mongoCollection.countDocuments()).isEqualTo(1);
    assertThat(mongoCollection.find().first().keySet())
        .containsExactlyInAnyOrder(
            ID_FIELD_NAME, DelegateAsyncTaskResponseKeys.responseData, DelegateAsyncTaskResponseKeys.processAfter);
  }
}
