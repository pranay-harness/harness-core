/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.changestreamsframework;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.changestreamsframework.ChangeEvent.ChangeEventBuilder;
import io.harness.exception.UnexpectedException;
import io.harness.persistence.PersistentEntity;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.types.ObjectId;

@OwnedBy(HarnessTeam.CE)
class ChangeEventFactory {
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
    //    return (String) uuidDocument.get("_id");
    if (uuidDocument.get("_id") instanceof String) {
      return (String) uuidDocument.get("_id");
    }
    return ((ObjectId) uuidDocument.get("_id")).toString();
  }

  private static ChangeType getChangeTypefromChangeStream(ChangeStreamDocument<DBObject> changeStreamDocument) {
    return ChangeType.valueOf(changeStreamDocument.getOperationType().getValue().toUpperCase());
  }

  private static DBObject getChangeDocumentfromChangeStream(ChangeStreamDocument<DBObject> changeStreamDocument) {
    BsonDocument changesBsonDocument = changeStreamDocument.getUpdateDescription().getUpdatedFields();
    return convertBsonDocumentToDBObject(changesBsonDocument);
  }

  private <T extends PersistentEntity> ChangeEvent<T> buildInsertChangeEvent(
      ChangeEventBuilder<T> changeEventBuilder, DBObject fullDocument) {
    changeEventBuilder.fullDocument(fullDocument);
    changeEventBuilder.changes(null);
    changeEventBuilder.changeType(ChangeType.INSERT);
    return changeEventBuilder.build();
  }

  private <T extends PersistentEntity> ChangeEvent<T> buildReplaceChangeEvent(
      ChangeEventBuilder<T> changeEventBuilder, DBObject fullDocument) {
    changeEventBuilder.fullDocument(fullDocument);
    changeEventBuilder.changes(null);
    changeEventBuilder.changeType(ChangeType.UPDATE);
    return changeEventBuilder.build();
  }

  private <T extends PersistentEntity> ChangeEvent<T> buildUpdateChangeEvent(
      ChangeEventBuilder<T> changeEventBuilder, ChangeStreamDocument<DBObject> changeStreamDocument) {
    DBObject dbObject = getChangeDocumentfromChangeStream(changeStreamDocument);
    changeEventBuilder.fullDocument(changeStreamDocument.getFullDocument());
    changeEventBuilder.changes(dbObject);
    changeEventBuilder.changeType(ChangeType.UPDATE);
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
      case INSERT:
        changeEvent = buildInsertChangeEvent(changeEventBuilder, changeStreamDocument.getFullDocument());
        break;
      case REPLACE:
        changeEvent = buildReplaceChangeEvent(changeEventBuilder, changeStreamDocument.getFullDocument());
        break;
      case UPDATE:
        changeEvent = buildUpdateChangeEvent(changeEventBuilder, changeStreamDocument);
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
