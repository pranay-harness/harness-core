package software.wings.service.impl;

import static com.google.common.collect.Sets.newHashSet;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.globalcontex.AuditGlobalContextData.AUDIT_ID;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static org.mongodb.morphia.query.Sort.descending;
import static software.wings.service.intfc.FileService.FileBucket;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.BasicDBObject;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.context.GlobalContextData;
import io.harness.exception.WingsException;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.manage.GlobalContextManager;
import io.harness.persistence.NameAccess;
import io.harness.persistence.ReadPref;
import io.harness.persistence.UuidAccess;
import io.harness.stream.BoundedInputStream;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.audit.AuditHeaderYamlResponse;
import software.wings.audit.AuditHeaderYamlResponse.AuditHeaderYamlResponseBuilder;
import software.wings.audit.EntityAuditRecord;
import software.wings.audit.EntityAuditRecord.EntityAuditRecordBuilder;
import software.wings.beans.EntityType;
import software.wings.beans.EntityYamlRecord;
import software.wings.beans.EntityYamlRecord.EntityYamlRecordKeys;
import software.wings.beans.Event.Type;
import software.wings.beans.FeatureName;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.yaml.YamlPayload;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  @Inject private FeatureFlagService featureFlagService;
  @Inject private EntityNameCache entityNameCache;
  @Inject private YamlResourceService yamlResourceService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;

  private WingsPersistence wingsPersistence;

  private static Set<String> nonYamlEntities =
      newHashSet(EntityType.TRIGGER.name(), EntityType.ROLE.name(), EntityType.TEMPLATE.name(),
          EntityType.TEMPLATE_FOLDER.name(), EntityType.ENCRYPTED_RECORDS.name(), EntityType.USER_GROUP.name());

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
  public AuditHeaderYamlResponse fetchAuditEntityYamls(String headerId, String entityId) {
    if (isEmpty(entityId)) {
      throw new WingsException("EntityId is needed.").addParam("message", "EntityId is needed.");
    }

    AuditHeader header = wingsPersistence.createQuery(AuditHeader.class)
                             .filter(AuditHeader.ID_KEY, headerId)
                             .project("entityAuditRecords", true)
                             .get();

    if (header == null) {
      throw new WingsException("Audit Header Id does not exists")
          .addParam("message", "Audit Header Id does not exists");
    }

    AuditHeaderYamlResponseBuilder builder =
        AuditHeaderYamlResponse.builder().auditHeaderId(headerId).entityId(entityId);

    if (isEmpty(header.getEntityAuditRecords())) {
      return builder.build();
    }

    Set<String> yamlIds = newHashSet();
    Optional<EntityAuditRecord> recordForPath =
        header.getEntityAuditRecords().stream().filter(record -> entityId.equals(record.getEntityId())).findFirst();
    if (!recordForPath.isPresent()) {
      return builder.build();
    }

    if (isNotEmpty(recordForPath.get().getEntityOldYamlRecordId())) {
      yamlIds.add(recordForPath.get().getEntityOldYamlRecordId());
    }
    if (isNotEmpty(recordForPath.get().getEntityNewYamlRecordId())) {
      yamlIds.add(recordForPath.get().getEntityNewYamlRecordId());
    }

    if (isNotEmpty(yamlIds)) {
      Query<EntityYamlRecord> query =
          wingsPersistence.createQuery(EntityYamlRecord.class).field(EntityYamlRecordKeys.uuid).in(yamlIds);
      List<EntityYamlRecord> entityAuditYamls = query.asList();
      if (isNotEmpty(entityAuditYamls)) {
        entityAuditYamls.forEach(yaml -> {
          if (yaml.getUuid().equals(recordForPath.get().getEntityOldYamlRecordId())) {
            builder.oldYaml(yaml.getYamlContent());
            builder.oldYamlPath(yaml.getYamlPath());
          } else if (yaml.getUuid().equals(recordForPath.get().getEntityNewYamlRecordId())) {
            builder.newYaml(yaml.getYamlContent());
            builder.newYamlPath(yaml.getYamlPath());
          }
        });
      }
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
    if (!featureFlagService.isEnabled(FeatureName.AUDIT_TRAIL, accountId)) {
      return;
    }

    try {
      String auditHeaderId = getAuditHeaderIdFromGlobalContext();
      if (isEmpty(auditHeaderId)) {
        throw new WingsException("AuditHeaderKey is null").addParam("message", "AuditHeaderKey is null");
      }

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
      EntityAuditRecord record = builder.build();
      updateEntityNameCacheIfRequired(oldEntity, newEntity, record);
      switch (type) {
        case CREATE: {
          record.setEntityNewYamlRecordId(saveEntityYamlForAudit(newEntity, record, accountId));
          break;
        }
        case UPDATE: {
          record.setEntityOldYamlRecordId(getLatestYamlRecordIdForEntity(record.getEntityType(), record.getEntityId()));
          record.setEntityNewYamlRecordId(saveEntityYamlForAudit(newEntity, record, accountId));
          break;
        }
        case DELETE: {
          record.setEntityOldYamlRecordId(getLatestYamlRecordIdForEntity(record.getEntityType(), record.getEntityId()));
          break;
        }
        default: {
          logger.warn(
              format("Unknown type class while registering audit actions: [%s]", type.getClass().getSimpleName()));
          return;
        }
      }
      UpdateOperations<AuditHeader> operations = wingsPersistence.createUpdateOperations(AuditHeader.class);
      operations.addToSet("entityAuditRecords", record);
      operations.set("accountId", accountId);
      wingsPersistence.update(
          wingsPersistence.createQuery(AuditHeader.class).filter(ID_KEY, auditHeaderId), operations);
    } catch (Exception ex) {
      logger.error(format("Exception while auditing records for account [%s]", accountId), ex);
    }
  }

  private <T> void updateEntityNameCacheIfRequired(T oldEntity, T newEntity, EntityAuditRecord record) {
    if (oldEntity instanceof NameAccess && newEntity instanceof NameAccess
        && !((NameAccess) oldEntity).getName().equals(((NameAccess) newEntity).getName())) {
      try {
        entityNameCache.invalidateCache(EntityType.valueOf(record.getEntityType()), record.getEntityId());
      } catch (Exception e) {
        logger.warn("Failed while invalidating EntityNameCache: " + e);
      }
    }
  }

  private String getAuditHeaderIdFromGlobalContext() throws Exception {
    GlobalContextData globalContextData = GlobalContextManager.get(AUDIT_ID);
    if (!(globalContextData instanceof AuditGlobalContextData)) {
      throw new Exception("Object of unknown class returned when querying for audit header Id");
    }
    return ((AuditGlobalContextData) globalContextData).getAuditId();
  }

  @VisibleForTesting
  String getLatestYamlRecordIdForEntity(String entityType, String entityId) {
    Key<EntityYamlRecord> key = wingsPersistence.createQuery(EntityYamlRecord.class)
                                    .filter(EntityYamlRecordKeys.entityId, entityId)
                                    .filter(EntityYamlRecordKeys.entityType, entityType)
                                    .order(descending(EntityYamlRecordKeys.createdAt))
                                    .getKey();
    return (key != null) ? key.getId().toString() : EMPTY;
  }

  @VisibleForTesting
  String saveEntityYamlForAudit(Object entity, EntityAuditRecord record, String accountId) {
    if (nonYamlEntities.contains(record.getEntityType()) || entity == null) {
      return EMPTY;
    }
    String yamlContent;
    String yamlPath;
    try {
      if (entity instanceof ServiceVariable) {
        if (EntityType.SERVICE.name().equals(record.getAffectedResourceType())) {
          entity = serviceResourceService.get(record.getAppId(), record.getAffectedResourceId());
        } else if (EntityType.ENVIRONMENT.name().equals(record.getAffectedResourceType())) {
          entity = environmentService.get(record.getAppId(), record.getAffectedResourceId(), false);
        } else {
          // Should ideally never happen
          return EMPTY;
        }
        YamlPayload resource = yamlResourceService.obtainEntityYamlVersion(accountId, entity).getResource();
        yamlContent = resource.getYaml();
      } else if (entity instanceof ManifestFile) {
        yamlContent = ((ManifestFile) entity).getFileContent();
      } else if (entity instanceof SettingAttribute
          && SettingVariableTypes.STRING.name().equals(((SettingAttribute) entity).getValue().getType())) {
        YamlPayload resource = yamlResourceService.getDefaultVariables(accountId, record.getAppId()).getResource();
        yamlContent = resource.getYaml();
      } else {
        YamlPayload resource = yamlResourceService.obtainEntityYamlVersion(accountId, entity).getResource();
        yamlContent = resource.getYaml();
      }
      yamlPath = entityHelper.getFullYamlPathForEntity(entity, record);
      record.setYamlPath(yamlPath);
    } catch (Exception ex) {
      yamlContent =
          format("Exception: [%s] while generating Yamls for entityId: [%s], entityType: [%s], accountId: [%s]",
              ex.getMessage(), record.getEntityId(), record.getEntityType(), accountId);
      yamlPath = EMPTY;
      logger.error(yamlContent, ex);
    }
    EntityYamlRecord yamlRecord = EntityYamlRecord.builder()
                                      .uuid(generateUuid())
                                      .createdAt(currentTimeMillis())
                                      .entityId(record.getEntityId())
                                      .entityType(record.getEntityType())
                                      .yamlPath(yamlPath)
                                      .yamlSha(sha1Hex(yamlContent))
                                      .yamlContent(yamlContent)
                                      .build();
    return wingsPersistence.save(yamlRecord);
  }
}