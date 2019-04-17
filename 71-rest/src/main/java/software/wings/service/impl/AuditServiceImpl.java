package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.k8s.model.AuditGlobalContextData.AUDIT_ID;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.service.intfc.FileService.FileBucket;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.BasicDBObject;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.context.GlobalContextData;
import io.harness.exception.WingsException;
import io.harness.k8s.model.AuditGlobalContextData;
import io.harness.manage.GlobalContextManager;
import io.harness.persistence.ReadPref;
import io.harness.persistence.UuidAccess;
import io.harness.stream.BoundedInputStream;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.audit.AuditHeaderYamlResponse;
import software.wings.audit.AuditHeaderYamlResponse.AuditHeaderYamlResponseBuilder;
import software.wings.audit.EntityAuditRecord;
import software.wings.audit.EntityAuditRecord.EntityAuditRecordBuilder;
import software.wings.beans.EntityYamlRecord;
import software.wings.beans.EntityYamlRecord.EntityYamlRecordKeys;
import software.wings.beans.Event.Type;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.FileService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Audit Service Implementation class.
 *
 * @author Rishi
 */
@Singleton
@Slf4j
public class AuditServiceImpl implements AuditService {
  @Inject private FileService fileService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private EntityHelper entityHelper;

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

  @Override
  public AuditHeaderYamlResponse fetchAuditEntityYamls(String headerId, String entityId, String entityType) {
    AuditHeader header = wingsPersistence.createQuery(AuditHeader.class)
                             .filter(AuditHeader.ID_KEY, headerId)
                             .project("entityAuditRecords", true)
                             .get();

    if (header == null) {
      throw new WingsException("Audit Header Id does not exists")
          .addParam("message", "Audit Header Id does not exists");
    }

    Set<String> yamlIds = new HashSet<>();
    AuditHeaderYamlResponseBuilder builder = AuditHeaderYamlResponse.builder().auditHeaderId(headerId);
    if (isEmpty(header.getEntityAuditRecords())) {
      return builder.build();
    }

    header.getEntityAuditRecords().forEach(entityAuditRecord -> {
      if (isNotEmpty(entityAuditRecord.getEntityNewYamlRecordId())) {
        yamlIds.add(entityAuditRecord.getEntityNewYamlRecordId());
      }
      if (isNotEmpty(entityAuditRecord.getEntityOldYamlRecordId())) {
        yamlIds.add(entityAuditRecord.getEntityOldYamlRecordId());
      }
    });

    if (isNotEmpty(yamlIds)) {
      Query<EntityYamlRecord> query =
          wingsPersistence.createQuery(EntityYamlRecord.class).field(EntityYamlRecordKeys.uuid).in(yamlIds);

      if (isNotBlank(entityId)) {
        if (isBlank(entityType)) {
          throw new WingsException("EntityType is required, when entityId is set.")
              .addParam("message", "EntityType is required, when entityId is set.");
        }

        query.filter("entityId", entityId).filter("entityType", entityType);
      }

      List<EntityYamlRecord> entityAuditYamls = query.asList();
      builder.entityAuditYamls(entityAuditYamls);
    }

    return builder.build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AuditHeader read(String appId, String auditHeaderId) {
    return wingsPersistence.getWithAppId(AuditHeader.class, appId, auditHeaderId);
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
    Query<AuditHeader> updateQuery = wingsPersistence.createQuery(AuditHeader.class).filter(ID_KEY, header.getUuid());
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
    final int batchSize = 1000;
    final int limit = 5000;
    final long days = TimeUnit.DAYS.convert(retentionMillis, TimeUnit.MILLISECONDS);
    logger.info("Start: Deleting audit records older than {} time", currentTimeMillis() - retentionMillis);
    try {
      logger.info("Start: Deleting audit records older than {} days", days);
      timeLimiter.callWithTimeout(() -> {
        while (true) {
          List<AuditHeader> auditHeaders = wingsPersistence.createQuery(AuditHeader.class, excludeAuthority)
                                               .field(AuditHeader.CREATED_AT_KEY)
                                               .lessThan(currentTimeMillis() - retentionMillis)
                                               .asList(new FindOptions().limit(limit).batchSize(batchSize));
          if (isEmpty(auditHeaders)) {
            logger.info("No more audit records older than {} days", days);
            return true;
          }
          try {
            logger.info("Deleting {} audit records", auditHeaders.size());

            List<ObjectId> requestPayloadIds =
                auditHeaders.stream()
                    .filter(auditHeader -> auditHeader.getRequestPayloadUuid() != null)
                    .map(auditHeader -> new ObjectId(auditHeader.getRequestPayloadUuid()))
                    .collect(toList());
            List<ObjectId> responsePayloadIds =
                auditHeaders.stream()
                    .filter(auditHeader -> auditHeader.getResponsePayloadUuid() != null)
                    .map(auditHeader -> new ObjectId(auditHeader.getResponsePayloadUuid()))
                    .collect(toList());
            wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "audits")
                .remove(new BasicDBObject(
                    ID_KEY, new BasicDBObject("$in", auditHeaders.stream().map(AuditHeader::getUuid).toArray())));

            if (requestPayloadIds != null) {
              wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "audits.files")
                  .remove(new BasicDBObject(ID_KEY, new BasicDBObject("$in", requestPayloadIds.toArray())));
              wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "audits.chunks")
                  .remove(new BasicDBObject("files_id", new BasicDBObject("$in", requestPayloadIds.toArray())));
            }

            if (responsePayloadIds != null) {
              wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "audits.files")
                  .remove(new BasicDBObject(ID_KEY, new BasicDBObject("$in", responsePayloadIds.toArray())));
              wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "audits.chunks")
                  .remove(new BasicDBObject("files_id", new BasicDBObject("$in", responsePayloadIds.toArray())));
            }
          } catch (Exception ex) {
            logger.warn(format("Failed to delete %d audit audit records", auditHeaders.size()), ex);
          }
          logger.info("Successfully deleted {} audit records", auditHeaders.size());
          if (auditHeaders.size() < limit) {
            return true;
          }
          sleep(ofSeconds(2L));
        }
      }, 10L, TimeUnit.MINUTES, true);
    } catch (Exception ex) {
      logger.warn(format("Failed to delete audit records older than last %d days within 10 minutes.", days), ex);
    }
    logger.info("Deleted audit records older than {} days", days);
  }

  @Override
  public <T> void registerAuditActions(String accountId, T oldEntity, T newEntity, Type type) {
    try {
      String auditHeaderId = getAuditHeaderIdFromGlobalContext();
      UuidAccess entityToQuery;
      switch (type) {
        case CREATE: {
          entityToQuery = (UuidAccess) newEntity;
          break;
        }
        case UPDATE: {
          entityToQuery = (UuidAccess) newEntity;
          break;
        }
        case DELETE: {
          entityToQuery = (UuidAccess) oldEntity;
          break;
        }
        default: {
          logger.warn(
              format("Unknown type class while registering audit actions: [%s]", type.getClass().getSimpleName()));
          return;
        }
      }
      EntityAuditRecordBuilder builder = EntityAuditRecord.builder();
      entityHelper.loadMetaDataForEntity(entityToQuery, builder, type);
      UpdateOperations<AuditHeader> operations = wingsPersistence.createUpdateOperations(AuditHeader.class);
      operations.addToSet("entityAuditRecords", builder.build());
      operations.set("accountId", accountId);
      wingsPersistence.update(
          wingsPersistence.createQuery(AuditHeader.class).filter(ID_KEY, auditHeaderId), operations);
    } catch (Exception ex) {
      logger.error(format("Exception while auditing records for account [%s]", accountId), ex);
    }
  }

  private String getAuditHeaderIdFromGlobalContext() throws Exception {
    GlobalContextData globalContextData = GlobalContextManager.get(AUDIT_ID);
    if (!(globalContextData instanceof AuditGlobalContextData)) {
      throw new Exception("Object of unknown class returned when querying for audit header Id");
    }
    return ((AuditGlobalContextData) globalContextData).getAuditId();
  }
}