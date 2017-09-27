package software.wings.service.impl.yaml;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.core.queue.Queue;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.ServiceYamlResourceService;
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.utils.Validator;
import software.wings.yaml.gitSync.EntityUpdateEvent;
import software.wings.yaml.gitSync.EntityUpdateEvent.SourceType;
import software.wings.yaml.gitSync.EntityUpdateListEvent;
import software.wings.yaml.gitSync.GitSyncFile;
import software.wings.yaml.gitSync.GitSyncHelper;
import software.wings.yaml.gitSync.YamlGitSync;
import software.wings.yaml.gitSync.YamlGitSync.SyncMode;
import software.wings.yaml.gitSync.YamlGitSync.Type;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

public class YamlGitSyncServiceImpl implements YamlGitSyncService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceYamlResourceService serviceYamlResourceService;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private SettingsService settingsService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private Queue<EntityUpdateEvent> entityUpdateEventQueue;

  /**
   * Gets the yaml git sync info by uuid
   *
   * @param uuid the uuid
   * @return the rest response
   */
  public YamlGitSync getByUuid(String uuid, String accountId, String appId) {
    YamlGitSync yamlGitSync = wingsPersistence.get(YamlGitSync.class, uuid);

    return yamlGitSync;
  }

  /**
   * Gets the yaml git sync info by entitytId
   *
   * @param entityId the uuid of the entity
   * @return the rest response
   */
  public YamlGitSync get(String entityId) {
    YamlGitSync yamlGitSync = wingsPersistence.createQuery(YamlGitSync.class).field("entityId").equal(entityId).get();

    return yamlGitSync;
  }

  /**
   * Gets the yaml git sync info by entitytId
   *
   * @param entityId the uuid of the entity
   * @param accountId the account id
   * @param appId the app id
   * @return the rest response
   */
  public YamlGitSync get(String entityId, String accountId, String appId) {
    YamlGitSync yamlGitSync = wingsPersistence.createQuery(YamlGitSync.class)
                                  .field("entityId")
                                  .equal(entityId)
                                  .field("accountId")
                                  .equal(accountId)
                                  .field("appId")
                                  .equal(appId)
                                  .get();

    return yamlGitSync;
  }

  /**
   * Gets the yaml git sync info by object type and entitytId
   *
   * @param type the object type
   * @param entityId the uuid of the entity
   * @param accountId the account id
   * @param appId the app id
   * @return the rest response
   */
  public YamlGitSync get(Type type, String entityId, @NotEmpty String accountId, String appId) {
    YamlGitSync yamlGitSync = wingsPersistence.createQuery(YamlGitSync.class)
                                  .field("accountId")
                                  .equal(accountId)
                                  .field("entityId")
                                  .equal(entityId)
                                  .field("type")
                                  .equal(type.name())
                                  .get();

    return yamlGitSync;
  }

  @Override
  public boolean exist(@NotEmpty Type type, @NotEmpty String entityId, @NotEmpty String accountId, String appId) {
    return wingsPersistence.createQuery(YamlGitSync.class)
               .field("accountId")
               .equal(accountId)
               .field("appId")
               .equal(appId)
               .field("entityId")
               .equal(entityId)
               .field("type")
               .equal(type.name())
               .getKey()
        != null;
  }

  /**
   * Creates a new yaml git sync info by object type and entitytId (uuid)
   *
   * @param accountId the account id
   * @param ygs the yamlGitSync info
   * @return the rest response
   */
  public YamlGitSync save(String accountId, String appId, YamlGitSync ygs) {
    Validator.notNullCheck("accountId", ygs.getAccountId());

    // check if it already exists
    if (exist(ygs.getType(), ygs.getEntityId(), accountId, appId)) {
      // do update instead
      return update(ygs.getEntityId(), accountId, appId, ygs);
    }

    YamlGitSync yamlGitSync = wingsPersistence.saveAndGet(YamlGitSync.class, ygs);

    // check to see if we need to push the initial yaml for this entity to synced Git repo
    if (ygs.getSyncMode() == SyncMode.HARNESS_TO_GIT || ygs.getSyncMode() == SyncMode.BOTH) {
      createEntityUpdateEvent(accountId, appId, ygs, SourceType.GIT_SYNC_CREATE);
    }

    return getByUuid(yamlGitSync.getUuid(), accountId, appId);
  }

  /**
   * Updates the yaml git sync info by object type and entitytId (uuid)
   *
   * @param entityId the uuid of the entity
   * @param accountId the account id
   * @param ygs the yamlGitSync info
   * @return the rest response
   */
  public YamlGitSync update(String entityId, String accountId, String appId, YamlGitSync ygs) {
    // check if it already exists
    if (exist(ygs.getType(), ygs.getEntityId(), accountId, appId)) {
      YamlGitSync yamlGitSync = get(ygs.getType(), ygs.getEntityId(), accountId, appId);

      Query<YamlGitSync> query =
          wingsPersistence.createQuery(YamlGitSync.class).field(ID_KEY).equal(yamlGitSync.getUuid());
      UpdateOperations<YamlGitSync> operations = wingsPersistence.createUpdateOperations(YamlGitSync.class)
                                                     .set("type", ygs.getType())
                                                     .set("enabled", ygs.isEnabled())
                                                     .set("url", ygs.getUrl())
                                                     .set("rootPath", ygs.getRootPath())
                                                     .set("sshKey", ygs.getSshKey())
                                                     .set("passphrase", ygs.getPassphrase())
                                                     .set("syncMode", ygs.getSyncMode());

      wingsPersistence.update(query, operations);

      // check to see if sync mode has changed such that we need to push the yaml to synced Git repo
      if (yamlGitSync.getSyncMode() == SyncMode.GIT_TO_HARNESS || yamlGitSync.getSyncMode() == null) {
        if (ygs.getSyncMode() == SyncMode.HARNESS_TO_GIT || ygs.getSyncMode() == SyncMode.BOTH) {
          createEntityUpdateEvent(accountId, appId, ygs, SourceType.GIT_SYNC_UPDATE);
        }
      }

      return wingsPersistence.get(YamlGitSync.class, yamlGitSync.getUuid());
    }

    return null;
  }

  public void createEntityUpdateEvent(String accountId, String appId, YamlGitSync ygs, SourceType sourceType) {
    String name = "";
    Class klass = null;

    switch (ygs.getType()) {
      case SETUP:
        name = "setup";
        klass = Account.class;
        break;
      case APP:
        Application app = appService.get(appId);
        name = app.getName();
        klass = Application.class;
        break;
      case SERVICE:
        Service service = serviceResourceService.get(appId, ygs.getEntityId());
        name = service.getName();
        klass = Service.class;
        break;
      case SERVICE_COMMAND:
        break;
      case ENVIRONMENT:
        Environment environment = environmentService.get(appId, ygs.getEntityId(), false);
        name = environment.getName();
        klass = Environment.class;
        break;
      case SETTING:
        SettingAttribute settingAttribute = settingsService.get(appId, ygs.getEntityId());
        name = settingAttribute.getName();
        klass = SettingAttribute.class;
        break;
      case WORKFLOW:
        Workflow workflow = workflowService.readWorkflow(appId, ygs.getEntityId());
        name = workflow.getName();
        klass = Workflow.class;
        break;
      case PIPELINE:
        Pipeline pipeline = pipelineService.readPipeline(appId, ygs.getEntityId(), false);
        name = pipeline.getName();
        klass = Pipeline.class;
        break;
      case TRIGGER:
        ArtifactStream artifactStream = artifactStreamService.get(appId, ygs.getEntityId());
        Service asService = serviceResourceService.get(appId, artifactStream.getServiceId());
        name = artifactStream.getSourceName() + "(" + asService.getName() + ")";
        klass = ArtifactStream.class;
        break;
      case FOLDER:
        // TODO - needs implementation
        break;
      default:
        // nothing to do
    }

    // queue an entity update event
    EntityUpdateEvent entityUpdateEvent = EntityUpdateEvent.Builder.anEntityUpdateEvent()
                                              .withEntityId(ygs.getEntityId())
                                              .withName(name)
                                              .withAccountId(accountId)
                                              .withAppId(appId)
                                              .withClass(klass)
                                              .withSourceType(sourceType)
                                              .build();
    entityUpdateEventQueue.send(entityUpdateEvent);
  }

  public boolean handleEntityUpdateListEvent(EntityUpdateListEvent entityUpdateListEvent) {
    // TODO - needs implementation

    // we need to sort the list of entity update events by the git repo they are synced to
    // map key = git sync URL
    Map<String, List<EntityUpdateEvent>> gitSyncUpdateMap = new HashMap<String, List<EntityUpdateEvent>>();

    for (EntityUpdateEvent eue : entityUpdateListEvent.getEntityUpdateEvents()) {
      YamlGitSync ygs = get(eue.getEntityId(), eue.getAccountId(), eue.getAppId());

      if (ygs != null) {
        String url = ygs.getUrl();

        if (gitSyncUpdateMap.containsKey(url)) {
          gitSyncUpdateMap.get(url).add(eue);
        } else {
          gitSyncUpdateMap.put(url, new ArrayList<EntityUpdateEvent>());
        }
      } else {
        // not a problem (error) - just means this entity is NOT git synced
      }
    }

    // now we can process each homogeneous list separately
    for (Map.Entry<String, List<EntityUpdateEvent>> entry : gitSyncUpdateMap.entrySet()) {
      System.out.println(entry.getKey() + ":" + entry.getValue());
      handleHomogeneousEntityUpdateList(entry.getValue());
    }

    return true;
  }

  public boolean handleHomogeneousEntityUpdateList(List<EntityUpdateEvent> entityUpdateEvents) {
    logger.info("*************** handleHomogeneousEntityUpdateList: " + entityUpdateEvents);

    if (entityUpdateEvents == null) {
      logger.info("ERROR: entityUpdateEvents are null!");
      return false;
    }

    if (entityUpdateEvents.size() == 0) {
      logger.info("ERROR: entityUpdateEvents are empty!");
      return false;
    }

    EntityUpdateEvent firstEue = entityUpdateEvents.get(0);
    String entityId = firstEue.getEntityId();
    YamlGitSync ygs = get(entityId, firstEue.getAccountId(), firstEue.getAppId());

    if (ygs == null) {
      // no git sync found for this entity
      return false;
    }

    File sshKeyPath = GitSyncHelper.getSshKeyPath(ygs.getSshKey(), entityId);
    GitSyncHelper gsh = new GitSyncHelper(ygs.getPassphrase(), sshKeyPath.getAbsolutePath());
    File repoPath = gsh.getTempRepoPath(entityId);
    // clone the repo
    gsh.clone(ygs.getUrl(), repoPath);

    List<GitSyncFile> gitSyncFiles = new ArrayList<GitSyncFile>();

    for (EntityUpdateEvent eue : entityUpdateEvents) {
      YamlGitSync eueYgs = get(eue.getEntityId(), eue.getAccountId(), eue.getAppId());

      if (eueYgs != null) {
        gitSyncFiles.add(GitSyncFile.Builder.aGitSyncFile()
                             .withName(eue.getName())
                             .withYaml(eue.getYaml())
                             .withSourceType(eue.getSourceType())
                             .withClass(eue.getKlass())
                             .withRootPath(eueYgs.getRootPath())
                             .build());
      } else {
        // TODO - handle missing eueYgs
      }
    }

    gsh.writeAddCommitPush(ygs, repoPath, gitSyncFiles);
    gsh.cleanupTempFiles(sshKeyPath, repoPath);
    gsh.shutdown();

    return true;
  }

  // TODO - when done adding support for EntityUpdateListEvent, we shouldn't need this - a "singleton" EntityUpdateEvent
  // will just be handled as a List with one item
  public boolean handleEntityUpdateEvent(EntityUpdateEvent entityUpdateEvent) {
    logger.info("*************** handleEntityUpdateEvent: " + entityUpdateEvent);

    String entityId = entityUpdateEvent.getEntityId();
    String name = entityUpdateEvent.getName();
    String accountId = entityUpdateEvent.getAccountId();
    String appId = entityUpdateEvent.getAppId();
    SourceType sourceType = entityUpdateEvent.getSourceType();
    Class klass = entityUpdateEvent.getKlass();
    String yaml = entityUpdateEvent.getYaml();

    if (entityId == null || entityId.isEmpty()) {
      logger.info("ERROR: EntityUpdateEvent entityId is missing!");
      return false;
    }

    if (sourceType == null) {
      logger.info("ERROR: EntityUpdateEvent sourceType is missing!");
      return false;
    }

    if (klass == null) {
      logger.info("ERROR: EntityUpdateEvent class is missing!");
      return false;
    }

    YamlGitSync ygs = get(entityId, accountId, appId);

    if (ygs == null) {
      // no git sync found for this entity
      return false;
    }

    File sshKeyPath = GitSyncHelper.getSshKeyPath(ygs.getSshKey(), entityId);
    GitSyncHelper gsh = new GitSyncHelper(ygs.getPassphrase(), sshKeyPath.getAbsolutePath());

    switch (sourceType) {
      case ENTITY_CREATE:
        // may need separate implementation - for now it "falls through" to GIT_SYNC_UPDATE
      case GIT_SYNC_CREATE:
        // may need separate implementation - for now it "falls through" to GIT_SYNC_UPDATE
      case ENTITY_UPDATE:
        // may need separate implementation - for now it "falls through" to GIT_SYNC_UPDATE
      case GIT_SYNC_UPDATE:
        File repoPath = gsh.getTempRepoPath(entityId);
        // clone the repo
        gsh.clone(ygs.getUrl(), repoPath);
        gsh.writeAddCommitPush(name, yaml, ygs, repoPath, sourceType, klass);
        gsh.cleanupTempFiles(sshKeyPath, repoPath);
        gsh.shutdown();

        return true;
      case GIT_SYNC_DELETE:
        // TODO - needs implementation!
        break;
      case ENTITY_DELETE:
        // TODO - needs implementation!
        break;
      default:
        // do nothing
    }

    return false;
  }
}
