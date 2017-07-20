package software.wings.service.impl;

import static org.awaitility.Awaitility.with;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.service.intfc.FileService.FileBucket;
import static software.wings.service.intfc.FileService.FileBucket.AUDITS;

import com.google.inject.Singleton;

import org.awaitility.Duration;
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
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
    return fileService.uploadFromStream(requestType + "-" + headerId, inputStream, FileBucket.AUDITS, metaData);
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
    int batchSize = 200;
    int limit = 1000;
    int days = (int) retentionMillis / (24 * 60 * 60 * 1000);
    logger.info("Start: Deleting audit records older than {} days", days);
    try {
      with().pollInterval(2, TimeUnit.SECONDS).await().atMost(Duration.FIVE_MINUTES).until(() -> {
        List<AuditHeader> auditHeaders = wingsPersistence.createQuery(AuditHeader.class)
                                             .limit(limit)
                                             .batchSize(batchSize)
                                             .field("createdAt")
                                             .lessThan(System.currentTimeMillis() - retentionMillis)
                                             .asList();
        if (auditHeaders.size() == 0) {
          logger.info("No audit records older than {} days", days);
          return true;
        }
        logger.info("Deleting audit records of size: {} ", auditHeaders.size());
        for (AuditHeader auditHeader : auditHeaders) {
          try {
            delete(auditHeader);
          } catch (Exception ex) {
            logger.info("Failed to delete audit record {} ", auditHeader);
          }
        }
        logger.info("Deleting audit records of size: {} success", auditHeaders.size());
        if (auditHeaders.size() < batchSize) {
          return true;
        }
        return false;
      });
    } catch (Exception ex) {
      logger.info("Failed to delete audit records older than last {} days", days);
    }
    logger.info("Deleted audit records  older than {} days", days);
  }
  private boolean delete(AuditHeader auditHeader) {
    boolean deleted = wingsPersistence.delete(auditHeader);
    if (deleted) {
      deleteAuditPayLoad(auditHeader);
    }
    return deleted;
  }

  private boolean deleteAuditPayLoad(AuditHeader auditHeader) {
    String requestPayloadUuid = auditHeader.getRequestPayloadUuid();
    if (requestPayloadUuid != null) {
      try {
        logger.info("Deleting Request Payload File {}", requestPayloadUuid);
        executorService.submit(() -> fileService.deleteFile(requestPayloadUuid, AUDITS));
      } catch (Exception ex) {
        logger.info("Failed to delete audit header request payload file {}", requestPayloadUuid);
      }
    }
    String responsePayloadUuid = auditHeader.getResponsePayloadUuid();
    if (responsePayloadUuid != null) {
      try {
        logger.info("Deleting Response Payload File {}", responsePayloadUuid);
        executorService.submit(() -> fileService.deleteFile(responsePayloadUuid, AUDITS));
      } catch (Exception ex) {
        logger.info("Failed to delete audit header response payload file {}", responsePayloadUuid);
      }
    }
    return false;
  }
}
