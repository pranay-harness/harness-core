package software.wings.service.impl;

import static org.awaitility.Awaitility.with;
import static org.awaitility.Duration.TEN_MINUTES;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.service.intfc.FileService.FileBucket;

import com.google.inject.Singleton;

import com.mongodb.BasicDBObject;
import org.apache.commons.collections.CollectionUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.FileService;
import software.wings.utils.BoundedInputStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Audit Service Implementation class.
 *
 * @author Rishi
 */
@Singleton
public class AuditServiceImpl implements AuditService {
  private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);

  @Inject private FileService fileService;
  private WingsPersistence wingsPersistence;
  @Inject private ExecutorService executorService;

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

  @Override
  public String create(AuditHeader header, RequestType requestType, InputStream inputStream) {
    String fileUuid = savePayload(header.getUuid(), requestType, inputStream);
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

  private String savePayload(String headerId, RequestType requestType, InputStream inputStream) {
    Map<String, Object> metaData = new HashMap<>();
    metaData.put("headerId", headerId);
    if (requestType != null) {
      metaData.put("requestType", requestType.name());
    }
    return fileService.uploadFromStream(
        requestType + "-" + headerId, new BoundedInputStream(inputStream), FileBucket.AUDITS, metaData);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateUser(AuditHeader header, User user) {
    if (header == null) {
      return;
    }
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
    String fileUuid = savePayload(auditHeader.getUuid(), RequestType.RESPONSE, new ByteArrayInputStream(payload));
    UpdateOperations<AuditHeader> ops = wingsPersistence.createUpdateOperations(AuditHeader.class)
                                            .set("responseStatusCode", header.getResponseStatusCode())
                                            .set("responseTime", header.getResponseTime());
    if (fileUuid != null) {
      ops = ops.set("responsePayloadUuid", fileUuid);
    }
    wingsPersistence.update(auditHeader, ops);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteAuditRecords(long retentionMillis) {
    int batchSize = 1000;
    int limit = 5000;
    long days = retentionMillis / (24 * 60 * 60 * 1000L);
    logger.info("Start: Deleting  audit records older than {} time", (System.currentTimeMillis() - retentionMillis));
    try {
      logger.info("Start: Deleting audit records less than {} days: ", days);
      with().pollInterval(2L, TimeUnit.SECONDS).await().atMost(TEN_MINUTES).until(() -> {
        List<AuditHeader> auditHeaders = wingsPersistence.createQuery(AuditHeader.class)
                                             .limit(limit)
                                             .batchSize(batchSize)
                                             .field("createdAt")
                                             .lessThan(System.currentTimeMillis() - retentionMillis)
                                             .asList();
        if (CollectionUtils.isEmpty(auditHeaders)) {
          logger.info("No more audit records older than {} days", days);
          return true;
        }
        try {
          logger.info("Deleting audit records of size: {} ", auditHeaders.size());

          List<String> auditHeaderIds = auditHeaders.stream().map(AuditHeader::getUuid).collect(Collectors.toList());
          List<ObjectId> requestPayloadIds =
              auditHeaders.stream()
                  .filter(auditHeader -> auditHeader.getRequestPayloadUuid() != null)
                  .map((AuditHeader auditHeader) -> new ObjectId(auditHeader.getRequestPayloadUuid()))
                  .collect(Collectors.toList());
          List<ObjectId> responsePayloadIds =
              auditHeaders.stream()
                  .filter(auditHeader -> auditHeader.getResponsePayloadUuid() != null)
                  .map((AuditHeader auditHeader) -> new ObjectId(auditHeader.getResponsePayloadUuid()))
                  .collect(Collectors.toList());
          wingsPersistence.getCollection("audits").remove(
              new BasicDBObject("_id", new BasicDBObject("$in", auditHeaderIds.toArray())));

          if (requestPayloadIds != null) {
            wingsPersistence.getCollection("audits.files")
                .remove(new BasicDBObject("_id", new BasicDBObject("$in", requestPayloadIds.toArray())));
            wingsPersistence.getCollection("audits.chunks")
                .remove(new BasicDBObject("files_id", new BasicDBObject("$in", requestPayloadIds.toArray())));
          }

          if (responsePayloadIds != null) {
            wingsPersistence.getCollection("audits.files")
                .remove(new BasicDBObject("_id", new BasicDBObject("$in", responsePayloadIds.toArray())));
            wingsPersistence.getCollection("audits.chunks")
                .remove(new BasicDBObject("files_id", new BasicDBObject("$in", responsePayloadIds.toArray())));
          }

        } catch (Exception ex) {
          logger.info("Failed to delete audit audit records of size: " + auditHeaders.size(), ex);
        }
        logger.info("Deleting audit records of size: {} success", auditHeaders.size());
        if (auditHeaders.size() < limit) {
          return true;
        }
        return false;
      });
    } catch (Exception ex) {
      logger.info(String.format("Failed to delete audit records older than last %s days within 10 minutes.", days), ex);
    }
    logger.info("Deleted audit records  older than {} days", days);
  }
}
