package software.wings.search.framework.changestreams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.harness.exception.UnexpectedException;
import io.harness.persistence.PersistentEntity;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.json.JsonWriterSettings;

import java.io.IOException;
import java.util.Map;

/**
 * The changeTask bean in which all the changes
 * received from ChangeTrackingTask are converted to.
 *
 * @author utkarsh
 */

@Slf4j
@Value
public class ChangeEvent {
  @NonNull private String token;
  @NonNull private ChangeType changeType;
  @NonNull private Class<? extends PersistentEntity> entityType;
  @NonNull private String uuid;
  private DBObject fullDocument;
  private DBObject changes;
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final JsonWriterSettings settings =
      JsonWriterSettings.builder().int64Converter((value, writer) -> writer.writeNumber(value.toString())).build();

  private static String getResumeTokenAsJson(ChangeStreamDocument<DBObject> changeStreamDocument) {
    return changeStreamDocument.getResumeToken().toJson(settings);
  }

  private static ChangeType getChangeType(ChangeStreamDocument<DBObject> changeStreamDocument) {
    return ChangeType.valueOf(changeStreamDocument.getOperationType().getValue().toUpperCase());
  }

  private static DBObject getChangeDocument(ChangeStreamDocument<DBObject> changeStreamDocument) {
    BsonDocument changesBsonDocument = changeStreamDocument.getUpdateDescription().getUpdatedFields();
    Codec<Document> codec = MongoClient.getDefaultCodecRegistry().get(Document.class);
    Document changesDocument =
        codec.decode(new BsonDocumentReader(changesBsonDocument), DecoderContext.builder().build());
    return new BasicDBObject(changesDocument);
  }

  public ChangeEvent(
      ChangeStreamDocument<DBObject> changeStreamDocument, Class<? extends PersistentEntity> entityType) {
    this.token = getResumeTokenAsJson(changeStreamDocument);
    this.changeType = getChangeType(changeStreamDocument);
    this.entityType = entityType;

    try {
      this.uuid =
          (String) mapper.readValue(changeStreamDocument.getDocumentKey().toJson(settings), Map.class).get("_id");
      switch (this.changeType) {
        case INSERT:
          this.fullDocument = changeStreamDocument.getFullDocument();
          this.changes = null;
          break;
        case UPDATE:
          this.fullDocument = changeStreamDocument.getFullDocument();
          this.changes = getChangeDocument(changeStreamDocument);
          break;
        case DELETE:
          this.fullDocument = null;
          this.changes = null;
          break;
        default:
          throw new UnexpectedException("Unknown OperationType received while processing ChangeStreamEvent");
      }
    } catch (IOException e) {
      logger.error(String.format("Failed to process the change %s", changeStreamDocument.toString()), e);
      throw new UnexpectedException(String.format("Failed to process the change %s", changeStreamDocument.toString()));
    }
  }
}