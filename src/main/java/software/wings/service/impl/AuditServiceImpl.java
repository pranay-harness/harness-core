package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Singleton;

import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.Document;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.FileService;

import java.io.ByteArrayInputStream;
import javax.inject.Inject;

/**
 * Audit Service Implementation class.
 *
 * @author Rishi
 */
@Singleton
public class AuditServiceImpl implements AuditService {
  @Inject private FileService fileService;
  private WingsPersistence wingsPersistence;

  /**
   * Instantiates a new audit service impl.
   *
   * @param wingsPersistence the wings persistence
   */
  @Inject
  public AuditServiceImpl(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<AuditHeader> list(PageRequest<AuditHeader> req) {
    return wingsPersistence.query(AuditHeader.class, req);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AuditHeader read(String appId, String auditHeaderId) {
    return wingsPersistence.get(AuditHeader.class, appId, auditHeaderId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AuditHeader create(AuditHeader header) {
    return wingsPersistence.saveAndGet(AuditHeader.class, header);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String create(AuditHeader header, RequestType requestType, byte[] httpBody) {
    String fileUuid = savePayload(header.getUuid(), requestType, httpBody);
    if (fileUuid != null) {
      UpdateOperations<AuditHeader> ops = wingsPersistence.createUpdateOperations(AuditHeader.class);
      if (requestType == RequestType.RESPONSE) {
        ops = ops.set("responsePayloadUuid", fileUuid);
      } else {
        ops = ops.set("requestPayloadUuid", fileUuid);
      }
      wingsPersistence.update(header, ops);
    }

    return fileUuid;
  }

  private String savePayload(String headerId, RequestType requestType, byte[] httpBody) {
    Document metadata = new Document();
    metadata.append("headerId", headerId);
    metadata.append("requestType", requestType == null ? null : requestType.name());
    GridFSUploadOptions options =
        new GridFSUploadOptions().chunkSizeBytes(1024 * 1024).metadata(metadata); // Fixme: merge with FileBucket helper
    String fileId = fileService.uploadFromStream(
        requestType + "-" + headerId, new ByteArrayInputStream(httpBody), FileService.FileBucket.AUDITS, options);
    return fileId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateUser(AuditHeader header, User user) {
    Query<AuditHeader> updateQuery =
        wingsPersistence.createQuery(AuditHeader.class).field(ID_KEY).equal(header.getUuid());
    UpdateOperations<AuditHeader> updateOperations =
        wingsPersistence.createUpdateOperations(AuditHeader.class).set("remoteUser", user);
    wingsPersistence.update(updateQuery, updateOperations);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void finalize(AuditHeader header, byte[] payload) {
    AuditHeader auditHeader = wingsPersistence.get(AuditHeader.class, header.getUuid());
    String fileUuid = savePayload(auditHeader.getUuid(), RequestType.RESPONSE, payload);
    UpdateOperations<AuditHeader> ops = wingsPersistence.createUpdateOperations(AuditHeader.class)
                                            .set("responseStatusCode", header.getResponseStatusCode())
                                            .set("responseTime", header.getResponseTime());
    if (fileUuid != null) {
      ops = ops.set("responsePayloadUuid", fileUuid);
    }
    wingsPersistence.update(auditHeader, ops);
  }
}
