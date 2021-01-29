package io.harness.changestreamsframework;

import io.harness.ChangeDataCaptureServiceConfig;
import io.harness.mongo.MongoModule;

import software.wings.beans.ce.CECloudAccount;

import com.google.inject.Inject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChangeDataCaptureJob implements Runnable {
  private MongoClient mongoClient;
  private MongoDatabase mongoDatabase;
  private ReadPreference readPreference;

  @Inject private ChangeDataCaptureServiceConfig mainConfiguration;

  @Override
  public void run() {
    log.info("In Change Data Capture Job");
    connectToMongoDatabase();
    MongoCollection<CECloudAccount> ceCloudAccounts =
        mongoDatabase.getCollection("ceCloudAccount", CECloudAccount.class);

    log.info("Starting Watch on CECloudAccount");
    ChangeStreamIterable<CECloudAccount> changeStream = ceCloudAccounts.watch();

    MongoCursor<ChangeStreamDocument<CECloudAccount>> cursor = changeStream.iterator();
    cursor.forEachRemaining(stream -> {
      CECloudAccount targetDoc = stream.getFullDocument();
      log.debug("Received ChangeStream: " + targetDoc);
    });
  }

  private MongoClientURI mongoClientUri() {
    final String mongoClientUrl = "mongodb://localhost:27017/harness";
    return new MongoClientURI(mongoClientUrl, MongoClientOptions.builder(MongoModule.defaultMongoClientOptions));
  }

  private void connectToMongoDatabase() {
    MongoClientURI uri = mongoClientUri();
    mongoClient = new MongoClient(uri);
    readPreference = ReadPreference.secondaryPreferred();
    final String databaseName = uri.getDatabase();
    log.info("Database is {}", databaseName);
    mongoDatabase =
        mongoClient.getDatabase(databaseName).withReadConcern(ReadConcern.MAJORITY).withReadPreference(readPreference);
  }
}
