package software.wings.service.impl;

import java.io.ByteArrayInputStream;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.UpdateOperations;

import com.mongodb.MongoClient;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;

import software.wings.beans.AuditHeader;
import software.wings.beans.AuditHeader.RequestType;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.dl.MongoHelper;
import software.wings.service.intfc.AuditService;

/**
 *  Audit Service Implementation class.
 *
 *
 * @author Rishi
 *
 */
public class AuditServiceImpl implements AuditService {
  private Datastore datastore;
  private GridFSBucket gridFSBucket;

  public AuditServiceImpl(Datastore datastore, MongoClient mongoClient, String db, String bucketName) {
    this.datastore = datastore;
    this.gridFSBucket = GridFSBuckets.create(mongoClient.getDatabase(db), bucketName);
  }

  @Override
  public PageResponse<AuditHeader> list(PageRequest<AuditHeader> req) {
    return MongoHelper.queryPageRequest(datastore, AuditHeader.class, req);
  }

  @Override
  public AuditHeader create(AuditHeader header) {
    Key<AuditHeader> key = datastore.save(header);
    return datastore.get(AuditHeader.class, key.getId());
  }

  @Override
  public String create(AuditHeader header, RequestType requestType, byte[] httpBody) {
    String fileUuid = savePayload(header.getUuid(), requestType, httpBody);
    if (fileUuid != null) {
      UpdateOperations<AuditHeader> ops = datastore.createUpdateOperations(AuditHeader.class);
      if (requestType == RequestType.RESPONSE) {
        ops = ops.set("responsePayloadUUID", fileUuid);
      } else {
        ops = ops.set("requestPayloadUUID", fileUuid);
      }
      datastore.update(header, ops);
    }

    return fileUuid;
  }

  private String savePayload(String headerId, RequestType requestType, byte[] httpBody) {
    Document metadata = new Document();
    metadata.append("headerId", headerId);
    metadata.append("requestType", requestType == null ? null : requestType.name());
    GridFSUploadOptions options = new GridFSUploadOptions().chunkSizeBytes(16 * 1024 * 1024).metadata(metadata);
    ObjectId fileId =
        gridFSBucket.uploadFromStream(requestType + "-" + headerId, new ByteArrayInputStream(httpBody), options);
    return fileId.toHexString();
  }

  @Override
  public void finalize(AuditHeader header, byte[] payload) {
    AuditHeader auditHeader = datastore.get(AuditHeader.class, header.getUuid());
    String fileUuid = savePayload(auditHeader.getUuid(), RequestType.RESPONSE, payload);
    UpdateOperations<AuditHeader> ops = datastore.createUpdateOperations(AuditHeader.class)
                                            .set("responseStatusCode", header.getResponseStatusCode())
                                            .set("responseTime", header.getResponseTime());
    if (fileUuid != null) {
      ops = ops.set("responsePayloadUUID", fileUuid);
    }
    datastore.update(auditHeader, ops);
  }
}
