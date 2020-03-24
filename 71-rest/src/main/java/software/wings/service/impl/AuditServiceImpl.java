package software.wings.service.impl;

import static com.google.common.collect.Sets.newHashSet;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
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
import static software.wings.service.intfc.security.SecretManager.CREATED_AT_KEY;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.BasicDBObject;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.context.GlobalContextData;
import io.harness.exception.WingsException;
import io.harness.exception.WingsException.ExecutionContext;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.logging.ExceptionLogger;
import io.harness.manage.GlobalContextManager;
import io.harness.persistence.HIterator;
import io.harness.persistence.NameAccess;
import io.harness.persistence.UuidAccess;
import io.harness.stream.BoundedInputStream;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.AuditHeaderKeys;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.audit.AuditHeaderYamlResponse;
import software.wings.audit.AuditHeaderYamlResponse.AuditHeaderYamlResponseBuilder;
import software.wings.audit.AuditRecord;
import software.wings.audit.AuditRecord.AuditRecordKeys;
import software.wings.audit.EntityAuditRecord;
import software.wings.audit.EntityAuditRecord.EntityAuditRecordBuilder;
import software.wings.audit.ResourceType;
import software.wings.beans.AuditPreference;
import software.wings.beans.EntityType;
import software.wings.beans.EntityYamlRecord;
import software.wings.beans.EntityYamlRecord.EntityYamlRecordKeys;
import software.wings.beans.Event.Type;
import software.wings.beans.FeatureName;
import software.wings.beans.HarnessTag;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.dl.WingsPersistence;
import software.wings.features.AuditTrailFeature;
import software.wings.features.api.RestrictedApi;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ResourceLookupService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.yaml.YamlPayload;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
  @Inject private ResourceLookupService resourceLookupService;
  @Inject private AuditPreferenceHelper auditPreferenceHelper;

  private WingsPersistence wingsPersistence;

  private static Set<String> nonYamlEntities = newHashSet(EntityType.TEMPLATE_FOLDER.name(),
      EntityType.ENCRYPTED_RECORDS.name(), EntityType.USER_GROUP.name(), ResourceType.CONNECTION_ATTRIBUTES.name(),
      ResourceType.DEPLOYMENT_FREEZE.name(), ResourceType.CUSTOM_DASHBOARD.name(), ResourceType.SECRET_MANAGER.name(),
      EntityType.PIPELINE_GOVERNANCE_STANDARD.name(), ResourceType.SSO_SETTINGS.name(), ResourceType.USER.name(),
      ResourceType.USER_INVITE.name(), ResourceType.DELEGATE.name(), ResourceType.DELEGATE_SCOPE.name(),
      ResourceType.DELEGATE_PROFILE.name());

  boolean checkIfYamlEntityIsBehindFeatureFlag(
      FeatureName featureName, String entityName, EntityAuditRecord record, String accountId) {
    if (featureFlagService.isEnabled(featureName, accountId)) {
      return record.getEntityType().equals(entityName) || record.getAffectedResourceType().equals(entityName);
    }
    return false;
  }

  /**
   * check for nonYamlEntites.
   */
  public boolean isNonYamlEntity(EntityAuditRecord record, String accountId) {
    // If any entity is to be hidden behind feature flag it can be hidden here.
    return nonYamlEntities.contains(record.getEntityType())
        || nonYamlEntities.contains(record.getAffectedResourceType());
  }

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
  @RestrictedApi(AuditTrailFeature.class)
  public PageResponse<AuditHeader> list(PageRequest<AuditHeader> req) {
    return wingsPersistence.query(AuditHeader.class, req);
  }

  @Override
  public AuditRecord fetchMostRecentAuditRecord(String auditHeaderId) {
    return wingsPersistence.createQuery(AuditRecord.class, excludeAuthority)
        .filter(AuditRecordKeys.auditHeaderId, auditHeaderId)
        .order(Sort.descending(CREATED_AT_KEY))
        .get();
  }

  @Override
  public List<AuditRecord> fetchEntityAuditRecordsOlderThanGivenTime(String auditHeaderId, long timestamp) {
    List<AuditRecord> auditRecords = new ArrayList<>();
    try (HIterator<AuditRecord> iterator =
             new HIterator<>(wingsPersistence.createQuery(AuditRecord.class, excludeAuthority)
                                 .filter(AuditRecordKeys.auditHeaderId, auditHeaderId)
                                 .field(AuditRecordKeys.createdAt)
                                 .lessThanOrEq(timestamp)
                                 .order(Sort.ascending(CREATED_AT_KEY))
                                 .fetch())) {
      while (iterator.hasNext()) {
        auditRecords.add(iterator.next());
      }
    }

    return auditRecords;
  }

  @Override
  public void addEntityAuditRecordsToSet(
      List<EntityAuditRecord> entityAuditRecords, String accountId, String auditHeaderId) {
    UpdateOperations<AuditHeader> operations = wingsPersistence.createUpdateOperations(AuditHeader.class);
    operations.addToSet(AuditHeaderKeys.entityAuditRecords, entityAuditRecords);
    // @TODO: Following line id needed for now. Once pr for stamping accountId at auditFilter level is merged, remove
    // following line.
    operations.set(AuditHeaderKeys.accountId, accountId);
    wingsPersistence.update(wingsPersistence.createQuery(AuditHeader.class).filter(ID_KEY, auditHeaderId), operations);
  }

  @Override
  public AuditHeaderYamlResponse fetchAuditEntityYamls(String headerId, String entityId, String accountId) {
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

    /**
     * Under single AuditHeader record, there can be multiple entityAuditRecords with same yamlPath.
     * e.g, User updates TriggerTags, then there will be 2 records for path,
     * "Setup/Applications/Tags/Triggers/[TRIGGER].yaml"
     * 1 created for actual trigger update and 1 created for trigger-tag update, but both will use same yamlPath
     * mentioned above.
     *
     * EntityAuditRecords are stored in the list in the order of update.
     * So to show complete diff that happened to TriggerYaml in this auditOperation, we need to pick oldYamlId from 1st
     * entityAuditRecord and newYamlId from last entityAuditRecord.
     *
     * Similar also applies for serviceVariables, as there will be multiple entityAuditRecords created for yamlPath
     * Setup/Application/[APP]/Service/[SERVICE]/Index.yaml
     */
    List<EntityAuditRecord> entityAuditRecords = header.getEntityAuditRecords()
                                                     .stream()
                                                     .filter(record -> entityId.equals(record.getEntityId()))
                                                     .collect(Collectors.toList());

    if (isEmpty(entityAuditRecords)) {
      return builder.build();
    }

    String entityType = entityAuditRecords.get(0).getEntityType();
    if (ResourceType.TAG.name().equals(entityType)) {
      entityAuditRecords = header.getEntityAuditRecords()
                               .stream()
                               .filter(record -> entityType.equals(record.getEntityType()))
                               .collect(Collectors.toList());
    }

    String entityOldYamlRecordId = entityAuditRecords.get(0).getEntityOldYamlRecordId();
    String entityNewYamlRecordId = entityAuditRecords.get(entityAuditRecords.size() - 1).getEntityNewYamlRecordId();

    Set<String> yamlIds = newHashSet();
    yamlIds.add(entityOldYamlRecordId);
    yamlIds.add(entityNewYamlRecordId);

    if (isNotEmpty(yamlIds)) {
      Query<EntityYamlRecord> query = wingsPersistence.createQuery(EntityYamlRecord.class)
                                          .filter(EntityYamlRecordKeys.accountId, accountId)
                                          .field(EntityYamlRecordKeys.uuid)
                                          .in(yamlIds);
      List<EntityYamlRecord> entityAuditYamls = query.asList();
      if (isNotEmpty(entityAuditYamls)) {
        entityAuditYamls.forEach(yaml -> {
          if (yaml.getUuid().equals(entityOldYamlRecordId)) {
            builder.oldYaml(yaml.getYamlContent());
            builder.oldYamlPath(yaml.getYamlPath());
          } else if (yaml.getUuid().equals(entityNewYamlRecordId)) {
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
            wingsPersistence.getCollection(DEFAULT_STORE, "audits")
                .remove(new BasicDBObject(
                    ID_KEY, new BasicDBObject("$in", auditHeaders.stream().map(AuditHeader::getUuid).toArray())));

            if (requestPayloadIds != null) {
              wingsPersistence.getCollection(DEFAULT_STORE, "audits.files")
                  .remove(new BasicDBObject(ID_KEY, new BasicDBObject("$in", requestPayloadIds.toArray())));
              wingsPersistence.getCollection(DEFAULT_STORE, "audits.chunks")
                  .remove(new BasicDBObject("files_id", new BasicDBObject("$in", requestPayloadIds.toArray())));
            }

            if (responsePayloadIds != null) {
              wingsPersistence.getCollection(DEFAULT_STORE, "audits.files")
                  .remove(new BasicDBObject(ID_KEY, new BasicDBObject("$in", responsePayloadIds.toArray())));
              wingsPersistence.getCollection(DEFAULT_STORE, "audits.chunks")
                  .remove(new BasicDBObject("files_id", new BasicDBObject("$in", responsePayloadIds.toArray())));
            }
          } catch (Exception ex) {
            logger.warn("Failed to delete {} audit records", auditHeaders.size(), ex);
          }
          logger.info("Successfully deleted {} audit records", auditHeaders.size());
          if (auditHeaders.size() < limit) {
            return true;
          }
          sleep(ofSeconds(2L));
        }
      }, 10L, TimeUnit.MINUTES, true);
    } catch (Exception ex) {
      logger.warn("Failed to delete audit records older than last {} days within 10 minutes.", days, ex);
    }
    logger.info("Deleted audit records older than {} days", days);
  }

  @Override
  public boolean deleteTempAuditRecords(List<String> ids) {
    return wingsPersistence.delete(wingsPersistence.createQuery(AuditRecord.class).field(AuditRecordKeys.uuid).in(ids));
  }

  @Override
  public <T> void handleEntityCrudOperation(String accountId, T oldEntity, T newEntity, Type type) {
    registerAuditActions(accountId, oldEntity, newEntity, type);
  }

  @Override
  public <T> void registerAuditActions(String accountId, T oldEntity, T newEntity, Type type) {
    try {
      String auditHeaderId = getAuditHeaderIdFromGlobalContext();
      if (isEmpty(auditHeaderId)) {
        throw new WingsException("AuditHeaderKey is null", USER).addParam("message", "AuditHeaderKey is null");
      }

      UuidAccess entityToQuery;
      switch (type) {
        case ENABLE_2FA:
        case DISABLE_2FA:
        case LINK_SSO:
        case UNLINK_SSO:
        case MODIFY_PERMISSIONS:
        case UPDATE_NOTIFICATION_SETTING:
        case CREATE:
        case ENABLE:
        case DISABLE:
        case LOCK:
        case UNLOCK:
        case RESET_PASSWORD:
        case UPDATE_SCOPE:
        case UPDATE_TAG:
        case ACCEPTED_INVITE:
        case TEST:
        case UPDATE:
          entityToQuery = (UuidAccess) newEntity;
          break;
        case DELETE:
          entityToQuery = (UuidAccess) oldEntity;
          break;
        default:
          logger.warn(
              format("Unknown type class while registering audit actions: [%s]", type.getClass().getSimpleName()));
          return;
      }
      EntityAuditRecordBuilder builder = EntityAuditRecord.builder();
      entityHelper.loadMetaDataForEntity(entityToQuery, builder, type);
      EntityAuditRecord record = builder.build();
      updateEntityNameCacheIfRequired(oldEntity, newEntity, record);
      switch (type) {
        case LOCK:
        case UNLOCK:
        case RESET_PASSWORD:
        case ACCEPTED_INVITE:
        case ENABLE_2FA:
        case DISABLE_2FA:
        case LINK_SSO:
        case UNLINK_SSO:
        case MODIFY_PERMISSIONS:
        case TEST:
        case UPDATE_NOTIFICATION_SETTING:
        case UPDATE_SCOPE:
        case CREATE: {
          saveEntityYamlForAudit(newEntity, record, accountId);
          resourceLookupService.updateResourceLookupRecordIfNeeded(record, accountId, newEntity, oldEntity);
          break;
        }
        case ENABLE:
        case DISABLE:
        case UPDATE_TAG:
        case UPDATE: {
          loadLatestYamlDetailsForEntity(record, accountId);
          saveEntityYamlForAudit(newEntity, record, accountId);
          resourceLookupService.updateResourceLookupRecordIfNeeded(record, accountId, newEntity, oldEntity);
          break;
        }
        case DELETE: {
          loadLatestYamlDetailsForEntity(record, accountId);
          resourceLookupService.deleteResourceLookupRecordIfNeeded(record, accountId);
          break;
        }
        default: {
          logger.warn(
              format("Unknown type class while registering audit actions: [%s]", type.getClass().getSimpleName()));
          return;
        }
      }

      if (featureFlagService.isEnabled(FeatureName.ENTITY_AUDIT_RECORD, accountId)) {
        long now = System.currentTimeMillis();
        // Setting createdAt in EntityAuditRecord
        record.setCreatedAt(now);
        AuditRecord auditRecord = AuditRecord.builder()
                                      .auditHeaderId(auditHeaderId)
                                      .entityAuditRecord(record)
                                      .createdAt(now)
                                      .accountId(accountId)
                                      .nextIteration(now + TimeUnit.MINUTES.toMillis(3))
                                      .build();

        wingsPersistence.save(auditRecord);
      } else {
        UpdateOperations<AuditHeader> operations = wingsPersistence.createUpdateOperations(AuditHeader.class);
        operations.addToSet("entityAuditRecords", record);
        operations.set("accountId", accountId);
        wingsPersistence.update(
            wingsPersistence.createQuery(AuditHeader.class).filter(ID_KEY, auditHeaderId), operations);
      }
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, ExecutionContext.MANAGER, logger);
    } catch (Exception ex) {
      logger.error("Exception while auditing records for account [{}]", accountId, ex);
    }
  }

  @Override
  @RestrictedApi(AuditTrailFeature.class)
  public PageResponse<AuditHeader> listUsingFilter(String accountId, String filterJson, String limit, String offset) {
    AuditPreference auditPreference = (AuditPreference) auditPreferenceHelper.parseJsonIntoPreference(filterJson);
    auditPreference.setAccountId(accountId);

    if (!auditPreference.isIncludeAppLevelResources() && !auditPreference.isIncludeAccountLevelResources()) {
      return new PageResponse<>();
    }

    PageRequest<AuditHeader> pageRequest =
        auditPreferenceHelper.generatePageRequestFromAuditPreference(auditPreference, offset, limit);
    return wingsPersistence.query(AuditHeader.class, pageRequest);
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
  void loadLatestYamlDetailsForEntity(EntityAuditRecord record, String accountId) {
    if (isNonYamlEntity(record, accountId)) {
      return;
    }
    String entityId;
    String entityType;
    if (EntityType.SERVICE_VARIABLE.name().equals(record.getEntityType())) {
      if (EntityType.SERVICE.name().equals(record.getAffectedResourceType())) {
        entityType = EntityType.SERVICE.name();
      } else if (EntityType.ENVIRONMENT.name().equals(record.getAffectedResourceType())) {
        entityType = EntityType.ENVIRONMENT.name();
      } else {
        // Should ideally never happen
        return;
      }
      entityId = record.getAffectedResourceId();
    } else {
      entityType = record.getEntityType();
      entityId = record.getEntityId();
    }

    // All Tags go to same yaml file, Tags.yaml. Ignore entityId in that case and use entity type to get all tags
    // updated in this audit record.
    Query<EntityYamlRecord> query = wingsPersistence.createQuery(EntityYamlRecord.class);
    if (!ResourceType.TAG.name().equals(entityType)) {
      query.filter(EntityYamlRecordKeys.entityId, entityId);
    }

    EntityYamlRecord entityYamlRecord = query.filter(EntityYamlRecordKeys.accountId, accountId)
                                            .filter(EntityYamlRecordKeys.entityType, entityType)
                                            .project(EntityYamlRecordKeys.uuid, true)
                                            .project(EntityYamlRecordKeys.yamlPath, true)
                                            .order(descending(EntityYamlRecordKeys.createdAt))
                                            .get();

    if (entityYamlRecord != null) {
      record.setYamlPath(entityYamlRecord.getYamlPath());
      record.setEntityOldYamlRecordId(entityYamlRecord.getUuid());
    }
  }

  @VisibleForTesting
  void saveEntityYamlForAudit(Object entity, EntityAuditRecord record, String accountId) {
    if (isNonYamlEntity(record, accountId) || entity == null) {
      return;
    }
    String yamlContent;
    String yamlPath;
    try {
      if (entity instanceof ServiceVariable) {
        if (EntityType.SERVICE.name().equals(record.getAffectedResourceType())) {
          entity = serviceResourceService.getWithDetails(record.getAppId(), record.getAffectedResourceId());
        } else if (EntityType.ENVIRONMENT.name().equals(record.getAffectedResourceType())) {
          entity = environmentService.get(record.getAppId(), record.getAffectedResourceId(), false);
        } else {
          // Should ideally never happen
          return;
        }
        YamlPayload resource = yamlResourceService.obtainEntityYamlVersion(accountId, entity).getResource();
        yamlContent = resource.getYaml();
      } else if (entity instanceof ManifestFile) {
        yamlContent = ((ManifestFile) entity).getFileContent();
      } else if (entity instanceof SettingAttribute
          && SettingVariableTypes.STRING.name().equals(((SettingAttribute) entity).getValue().getType())) {
        YamlPayload resource = yamlResourceService.getDefaultVariables(accountId, record.getAppId()).getResource();
        yamlContent = resource.getYaml();
      } else if (entity instanceof HarnessTag) {
        yamlContent = yamlResourceService.getHarnessTags(accountId).getResource().getYaml();
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
    String entityId;
    String entityType;
    if (EntityType.SERVICE_VARIABLE.name().equals(record.getEntityType())) {
      if (EntityType.SERVICE.name().equals(record.getAffectedResourceType())) {
        entityType = EntityType.SERVICE.name();
      } else if (EntityType.ENVIRONMENT.name().equals(record.getAffectedResourceType())) {
        entityType = EntityType.ENVIRONMENT.name();
      } else {
        // Should ideally never happen
        return;
      }
      entityId = record.getAffectedResourceId();
    } else {
      entityType = record.getEntityType();
      entityId = record.getEntityId();
    }
    EntityYamlRecord yamlRecord = EntityYamlRecord.builder()
                                      .uuid(generateUuid())
                                      .accountId(accountId)
                                      .createdAt(currentTimeMillis())
                                      .entityId(entityId)
                                      .entityType(entityType)
                                      .yamlPath(yamlPath)
                                      .yamlSha(sha1Hex(yamlContent))
                                      .yamlContent(yamlContent)
                                      .build();
    String newYamlId = wingsPersistence.save(yamlRecord);
    record.setEntityNewYamlRecordId(newYamlId);
  }
}