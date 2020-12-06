package software.wings.search.framework.changestreams;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.persistence.PersistentEntity;

import software.wings.dl.WingsPersistence;
import software.wings.search.framework.changestreams.ChangeEvent.ChangeEventBuilder;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;

@OwnedBy(PL)
class ChangeEventFactory {
  @Inject private WingsPersistence wingsPersistence;

  private static Document convertBsonDocumentToDocument(BsonDocument bsonDocument) {
    Codec<Document> codec = MongoClient.getDefaultCodecRegistry().get(Document.class);
    return codec.decode(new BsonDocumentReader(bsonDocument), DecoderContext.builder().build());
  }

  private static DBObject convertBsonDocumentToDBObject(BsonDocument bsonDocument) {
    Document changesDocument = convertBsonDocumentToDocument(bsonDocument);
    return new BasicDBObject(changesDocument);
  }

  private static String getResumeTokenAsJson(ChangeStreamDocument<DBObject> changeStreamDocument) {
    return changeStreamDocument.getResumeToken().toJson();
  }

  private static String getUuidfromChangeStream(ChangeStreamDocument<DBObject> changeStreamDocument) {
    Document uuidDocument = convertBsonDocumentToDocument(changeStreamDocument.getDocumentKey());
    return (String) uuidDocument.get("_id");
  }

  private static ChangeType getChangeTypefromChangeStream(ChangeStreamDocument<DBObject> changeStreamDocument) {
    return ChangeType.valueOf(changeStreamDocument.getOperationType().getValue().toUpperCase());
  }

  private static DBObject getChangeDocumentfromChangeStream(ChangeStreamDocument<DBObject> changeStreamDocument) {
    BsonDocument changesBsonDocument = changeStreamDocument.getUpdateDescription().getUpdatedFields();
    return convertBsonDocumentToDBObject(changesBsonDocument);
  }

  private <T extends PersistentEntity> ChangeEvent<T> buildInsertChangeEvent(
      ChangeEventBuilder<T> changeEventBuilder, DBObject fullDocument, Class<T> entityClass) {
    T entityObject = wingsPersistence.convertToEntity(entityClass, fullDocument);
    changeEventBuilder.fullDocument(entityObject);
    changeEventBuilder.changes(null);
    changeEventBuilder.changeType(ChangeType.INSERT);
    return changeEventBuilder.build();
  }

  private <T extends PersistentEntity> ChangeEvent<T> buildUpdateChangeEvent(ChangeEventBuilder<T> changeEventBuilder,
      ChangeStreamDocument<DBObject> changeStreamDocument, Class<T> entityClass) {
    T fullDocument = wingsPersistence.convertToEntity(entityClass, changeStreamDocument.getFullDocument());
    DBObject dbObject = getChangeDocumentfromChangeStream(changeStreamDocument);
    changeEventBuilder.fullDocument(fullDocument);
    changeEventBuilder.changes(dbObject);
    return changeEventBuilder.build();
  }

  <T extends PersistentEntity> ChangeEvent<T> fromChangeStreamDocument(
      ChangeStreamDocument<DBObject> changeStreamDocument, Class<T> entityClass) {
    ChangeType changeType = getChangeTypefromChangeStream(changeStreamDocument);

    ChangeEventBuilder<T> changeEventBuilder = ChangeEvent.builder();
    changeEventBuilder.entityType(entityClass);
    changeEventBuilder.changeType(changeType);
    changeEventBuilder.token(getResumeTokenAsJson(changeStreamDocument));
    changeEventBuilder.uuid(getUuidfromChangeStream(changeStreamDocument));

    ChangeEvent<T> changeEvent;
    switch (changeType) {
      case REPLACE:
      case INSERT:
        changeEvent = buildInsertChangeEvent(changeEventBuilder, changeStreamDocument.getFullDocument(), entityClass);
        break;
      case UPDATE:
        changeEvent = buildUpdateChangeEvent(changeEventBuilder, changeStreamDocument, entityClass);
        break;
      case DELETE:
        changeEvent = changeEventBuilder.build();
        break;
      case INVALIDATE:
        throw new UnexpectedException(String.format(
            "Invalidate event received. Should not have happened. Event %s", changeStreamDocument.toString()));
      default:
        throw new UnsupportedOperationException("Unknown OperationType received while processing ChangeStreamEvent");
    }
    return changeEvent;
  }
}
