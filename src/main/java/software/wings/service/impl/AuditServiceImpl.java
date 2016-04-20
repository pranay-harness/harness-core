package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Singleton;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AuditService;

import java.io.ByteArrayInputStream;
import javax.inject.Inject;

/**
 * Audit Service Implementation class.
 *
 * @author Rishi
 */
@Singleton
public class AuditServiceImpl implements AuditService {
  private static final String AUDIT_BUCKET = "audits";

  private WingsPersistence wingsPersistence;

  private GridFSBucket gridFSBucket;

  @Inject
  public AuditServiceImpl(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
    this.gridFSBucket = wingsPersistence.createGridFSBucket(AUDIT_BUCKET);
  }

  @Override
  public AuditHeader create(AuditHeader header) {
    return wingsPersistence.saveAndGet(AuditHeader.class, header);
  }

  @Override
  public void finalize(AuditHeader header, byte[] payload) {
    AuditHeader auditHeader = wingsPersistence.get(AuditHeader.class, header.getUuid());
    String fileUuid = savePayload(auditHeader.getUuid(), RequestType.RESPONSE, payload);
    UpdateOperations<AuditHeader> ops = wingsPersistence.createUpdateOperations(AuditHeader.class)
                                            .set("responseStatusCode", header.getResponseStatusCode())
                                            .set("responseTime", header.getResponseTime());
    if (fileUuid != null) {
      ops = ops.set("responsePayloadUUID", fileUuid);
    }
    wingsPersistence.update(auditHeader, ops);
  }

  @Override
  public PageResponse<AuditHeader> list(PageRequest<AuditHeader> req) {
    return wingsPersistence.query(AuditHeader.class, req);
  }

  @Override
  public String create(AuditHeader header, RequestType requestType, byte[] httpBody) {
    String fileUuid = savePayload(header.getUuid(), requestType, httpBody);
    if (fileUuid != null) {
      UpdateOperations<AuditHeader> ops = wingsPersistence.createUpdateOperations(AuditHeader.class);
      if (requestType == RequestType.RESPONSE) {
        ops = ops.set("responsePayloadUUID", fileUuid);
      } else {
        ops = ops.set("requestPayloadUUID", fileUuid);
      }
      wingsPersistence.update(header, ops);
    }

    return fileUuid;
  }

  @Override
  public void updateUser(AuditHeader header, User user) {
    Query<AuditHeader> updateQuery =
        wingsPersistence.createQuery(AuditHeader.class).field(ID_KEY).equal(header.getUuid());
    UpdateOperations<AuditHeader> updateOperations =
        wingsPersistence.createUpdateOperations(AuditHeader.class).set("remoteUser", user);
    wingsPersistence.update(updateQuery, updateOperations);
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
}
