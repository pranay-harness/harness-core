package io.harness.service;

import static com.mongodb.DBCollection.ID_FIELD_NAME;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
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
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.service.impl.DelegateCallbackRegistryImpl;
import io.harness.service.intfc.DelegateTaskResultsProvider;
import io.harness.waiter.StringNotifyResponseData;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.InetSocketAddress;

public class MongoDelegateTaskResultsProviderTest extends DelegateServiceTestBase {
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
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void test() {
    MongoClient mongoClient = new MongoClient(new MongoClientURI(getMongoUri()));
    mongoClient.getDatabase("harness").createCollection("cx_delegateAsyncTaskResponses");
    MongoCollection<Document> mongoCollection =
        mongoClient.getDatabase("harness").getCollection("cx_delegateAsyncTaskResponses");

    byte[] expectedTaskResults = kryoSerializer.asDeflatedBytes(StringNotifyResponseData.builder().data("OK").build());

    Document document = new Document();
    document.put(ID_FIELD_NAME, "taskId");
    document.put(DelegateAsyncTaskResponseKeys.responseData, expectedTaskResults);
    document.put(DelegateAsyncTaskResponseKeys.processAfter, 0);
    mongoCollection.insertOne(document);

    DelegateCallback delegateCallback =
        DelegateCallback.newBuilder()
            .setMongoDatabase(
                MongoDatabase.newBuilder().setConnection(getMongoUri()).setCollectionNamePrefix("cx").build())
            .build();
    String driverId = delegateCallbackRegistry.ensureCallback(delegateCallback);
    DelegateTaskResultsProvider delegateCallbackService =
        delegateCallbackRegistry.obtainDelegateTaskResultsProvider(driverId);
    byte[] taskResults = new byte[0];
    try {
      taskResults = delegateCallbackService.getDelegateTaskResults("taskId");
    } catch (Exception e) {
      fail(e.getMessage());
    }

    assertThat(taskResults).isEqualTo(expectedTaskResults);
  }
}
