package io.harness.aggregator;

import io.harness.aggregator.models.MongoReconciliationOffset;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.runtime.WorkerConfig;
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PL)
@NoArgsConstructor
@Slf4j
public class MongoOffsetBackingStore extends MemoryOffsetBackingStore {
  private MongoTemplate mongoTemplate;

  @Override
  public void configure(WorkerConfig workerConfig) {
    super.configure(workerConfig);
    String connectionUri = workerConfig.getString("offset.storage.file.filename");
    MongoClientOptions primaryMongoClientOptions = MongoClientOptions.builder()
                                                       .retryWrites(true)
                                                       .connectTimeout(30000)
                                                       .serverSelectionTimeout(90000)
                                                       .maxConnectionIdleTime(600000)
                                                       .connectionsPerHost(300)
                                                       .readPreference(ReadPreference.primary())
                                                       .build();
    MongoClientURI uri = new MongoClientURI(connectionUri, MongoClientOptions.builder(primaryMongoClientOptions));
    mongoTemplate = new MongoTemplate(new MongoClient(uri), Objects.requireNonNull(uri.getDatabase()));
  }

  @Override
  public void start() {
    super.start();
    log.info("Starting Mongo offset backing store...");
    load();
  }

  private void load() {
    this.data = new HashMap<>();
    MongoReconciliationOffset mongoReconciliationOffset =
        mongoTemplate.findOne(new Query().with(Sort.by(Sort.Order.desc(MongoReconciliationOffset.keys.createdAt))),
            MongoReconciliationOffset.class);
    if (mongoReconciliationOffset != null) {
      this.data.put(
          ByteBuffer.wrap(mongoReconciliationOffset.getKey()), ByteBuffer.wrap(mongoReconciliationOffset.getValue()));
    } else {
      log.info("No offset found in the database, will start a full sync.");
    }
  }

  @Override
  public void stop() {
    super.stop();
    log.info("Stopped Mongo offset backing store...");
  }

  @Override
  protected void save() {
    for (Map.Entry<ByteBuffer, ByteBuffer> mapEntry : data.entrySet()) {
      byte[] key = (mapEntry.getKey() != null) ? mapEntry.getKey().array() : null;
      byte[] value = (mapEntry.getValue() != null) ? mapEntry.getValue().array() : null;
      MongoReconciliationOffset mongoReconciliationOffset = mongoTemplate.save(
          MongoReconciliationOffset.builder().key(key).value(value).createdAt(System.currentTimeMillis()).build());
      log.info("Saved offset in db is: {}", mongoReconciliationOffset);
    }
  }
}
