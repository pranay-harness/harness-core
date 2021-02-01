package io.harness.changestreamsframework;

import io.harness.ChangeDataCaptureServiceConfig;
import io.harness.mongo.MongoModule;

import software.wings.beans.ce.CECloudAccount;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.Strings;
import org.bson.BsonDocument;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

@Slf4j
public class ChangeDataCaptureJob implements Runnable {
  private MongoClient mongoClient;
  private MongoDatabase mongoDatabase;
  private ReadPreference readPreference;

  @Inject private ChangeDataCaptureServiceConfig mainConfiguration;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void run() {
    log.info("In Change Data Capture Job");
    connectToMongoDatabase();
    MongoCollection<DBObject> ceCloudAccounts = mongoDatabase.getCollection("ceCloudAccount")
                                                    .withDocumentClass(DBObject.class)
                                                    .withReadPreference(readPreference);
    log.info("Starting Watch on CECloudAccount");
    ChangeStreamIterable<DBObject> changeStream = ceCloudAccounts.watch();

    try {
      MongoCursor<ChangeStreamDocument<DBObject>> cursor = changeStream.iterator();
      cursor.forEachRemaining(stream -> {
        String operationType = Strings.toUpperCase(stream.getOperationType().getValue());
        BsonDocument documentKey = stream.getDocumentKey();
        DBObject targetDoc = stream.getFullDocument();
        log.info("Received ChangeStream with Operation Type {}: {}", operationType, targetDoc);
        log.info("Document Key: {}", documentKey);

        if (!operationType.equals("DELETE")) {
          CECloudAccount ceCloudAccount = wingsPersistence.convertToEntity(CECloudAccount.class, targetDoc);
          log.info("CECloudAccount: {}", ceCloudAccount);
        }
      });
    } catch (Exception e) {
      log.error("Exception While Watching Change Stream {}", e);
    }
  }

  private MongoClientURI mongoClientUri() {
    final String mongoClientUrl = mainConfiguration.getHarnessMongo().getUri();
    return new MongoClientURI(mongoClientUrl, MongoClientOptions.builder(MongoModule.defaultMongoClientOptions));
  }

  private void connectToMongoDatabase() {
    MongoClientURI uri = mongoClientUri();
    mongoClient = new MongoClient(uri);
    readPreference = ReadPreference.secondaryPreferred();
    CodecRegistry pojoCodecRegistry =
        org.bson.codecs.configuration.CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
            org.bson.codecs.configuration.CodecRegistries.fromProviders(
                PojoCodecProvider.builder().automatic(true).build()));
    final String databaseName = uri.getDatabase();
    log.info("Database is {}", databaseName);
    mongoDatabase = mongoClient.getDatabase(databaseName)
                        .withReadConcern(ReadConcern.MAJORITY)
                        .withCodecRegistry(pojoCodecRegistry)
                        .withReadPreference(readPreference);
  }
}
