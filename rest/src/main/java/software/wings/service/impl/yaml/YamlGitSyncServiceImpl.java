package software.wings.service.impl.yaml;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.yaml.gitSync.YamlGitSync.Type.SETUP;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.ResponseMessage;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.AppYamlResourceService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.ServiceYamlResourceService;
import software.wings.service.intfc.yaml.SetupYamlResourceService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.utils.CryptoUtil;
import software.wings.utils.Validator;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.gitSync.EntityUpdateEvent;
import software.wings.yaml.gitSync.EntityUpdateEvent.SourceType;
import software.wings.yaml.gitSync.EntityUpdateListEvent;
import software.wings.yaml.gitSync.GitSyncFile;
import software.wings.yaml.gitSync.GitSyncHelper;
import software.wings.yaml.gitSync.GitSyncWebhook;
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
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private SetupYamlResourceService setupYamlResourceService;
  @Inject private AppYamlResourceService appYamlResourceService;
  @Inject private YamlResourceService yamlResourceService;
  @Inject private CommandService commandService;
  @Inject private YamlDirectoryService yamlDirectoryService;

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
      // createEntityUpdateListEvent(accountId, appId, ygs, SourceType.GIT_SYNC_CREATE);

      logger.info("******* YamlGitSync save");

      // if it is Setup - we need to pushDirectory
      if (ygs.getEntityId().equals("setup")) {
        logger.info("******* Calling yamlDirectoryService.pushDirectory");

        yamlDirectoryService.pushDirectory(accountId, false);
      }
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
          // createEntityUpdateListEvent(accountId, appId, ygs, SourceType.GIT_SYNC_UPDATE);

          // if it is Setup - we need to pushDirectory
          if (ygs.getType() == SETUP) {
            yamlDirectoryService.pushDirectory(accountId, false);
          }
        }
      }

      return wingsPersistence.get(YamlGitSync.class, yamlGitSync.getUuid());
    }

    return null;
  }

  public void createEntityUpdateListEvent(String accountId, String appId, YamlGitSync ygs, SourceType sourceType) {
    String name = "";
    Class klass = null;
    String yaml = "";

    switch (ygs.getType()) {
      case SETUP:
        name = "setup";
        klass = Account.class;
        yaml = setupYamlResourceService.getSetup(accountId).getResource().getYaml();
        break;
      case APP:
        Application app = appService.get(appId);
        name = app.getName();
        klass = Application.class;
        yaml = appYamlResourceService.getApp(appId).getResource().getYaml();
        break;
      case SERVICE:
        Service service = serviceResourceService.get(appId, ygs.getEntityId());
        name = service.getName();
        klass = Service.class;
        yaml = serviceYamlResourceService.getServiceYaml(service);
        break;
      case SERVICE_COMMAND:
        ServiceCommand serviceCommand = commandService.getServiceCommand(appId, ygs.getEntityId());
        name = serviceCommand.getName();
        klass = ServiceCommand.class;
        yaml = yamlResourceService.getServiceCommand(appId, serviceCommand.getUuid()).getResource().getYaml();
        break;
      case ENVIRONMENT:
        Environment environment = environmentService.get(appId, ygs.getEntityId(), false);
        name = environment.getName();
        klass = Environment.class;
        yaml = yamlResourceService.getEnvironment(appId, environment.getUuid()).getResource().getYaml();
        break;
      case SETTING:
        SettingAttribute settingAttribute = settingsService.get(appId, ygs.getEntityId());
        name = settingAttribute.getName();
        klass = SettingAttribute.class;
        yaml = yamlResourceService.getSettingAttribute(accountId, settingAttribute.getUuid()).getResource().getYaml();
        break;
      case WORKFLOW:
        Workflow workflow = workflowService.readWorkflow(appId, ygs.getEntityId());
        name = workflow.getName();
        klass = Workflow.class;
        yaml = yamlResourceService.getWorkflow(appId, workflow.getUuid()).getResource().getYaml();
        break;
      case PIPELINE:
        Pipeline pipeline = pipelineService.readPipeline(appId, ygs.getEntityId(), false);
        name = pipeline.getName();
        klass = Pipeline.class;
        yaml = yamlResourceService.getPipeline(appId, pipeline.getUuid()).getResource().getYaml();
        break;
      case TRIGGER:
        ArtifactStream artifactStream = artifactStreamService.get(appId, ygs.getEntityId());
        Service asService = serviceResourceService.get(appId, artifactStream.getServiceId());
        name = artifactStream.getSourceName() + "(" + asService.getName() + ")";
        klass = ArtifactStream.class;
        yaml = yamlResourceService.getTrigger(appId, artifactStream.getUuid()).getResource().getYaml();
        break;
      case FOLDER:
        // TODO - not sure what is correct (ultimately) for this
        name = ygs.getDirectoryPath();
        klass = FolderNode.class;
        yaml = "";
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
                                              .withYaml(yaml)
                                              .build();

    // create an EntityUpdateListEvent and queue it
    EntityUpdateListEvent eule = new EntityUpdateListEvent();
    eule.addEntityUpdateEvent(entityUpdateEvent);
    entityUpdateService.queueEntityUpdateList(eule);
  }

  public boolean handleEntityUpdateListEvent(EntityUpdateListEvent entityUpdateListEvent) {
    logger.info(
        "*************** handleTreeSync: entityUpdateListEvent.isTreeSync() = " + entityUpdateListEvent.isTreeSync());

    if (entityUpdateListEvent.isTreeSync()) {
      if (entityUpdateListEvent.getEntityUpdateEvents().size() == 1) {
        return handleTreeSync(entityUpdateListEvent);
      } else {
        // TODO - treeSyncs should only have one EntityUpdateEvent!
      }
    }

    // we need to sort the list of entity update events by the git repo they are synced to
    // map key = git sync URL
    Map<String, List<EntityUpdateEvent>> gitSyncUpdateMap = new HashMap<String, List<EntityUpdateEvent>>();

    List<EntityUpdateEvent> entityUpdateEvents = entityUpdateListEvent.getEntityUpdateEvents();

    for (EntityUpdateEvent eue : entityUpdateEvents) {
      YamlGitSync ygs = get(eue.getEntityId(), eue.getAccountId(), eue.getAppId());

      if (eue.getAppId() == null || eue.getAppId().isEmpty() || eue.getAppId().equals(GLOBAL_APP_ID)) {
        ygs = get(eue.getEntityId());
      }

      if (ygs != null) {
        String url = ygs.getUrl();

        if (gitSyncUpdateMap.containsKey(url)) {
          gitSyncUpdateMap.get(url).add(eue);
        } else {
          List<EntityUpdateEvent> tempList = new ArrayList<EntityUpdateEvent>();
          tempList.add(eue);
          gitSyncUpdateMap.put(url, tempList);
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

  public boolean handleTreeSync(EntityUpdateListEvent entityUpdateListEvent) {
    logger.info("*************** handleTreeSync");

    List<EntityUpdateEvent> entityUpdateEvents = entityUpdateListEvent.getEntityUpdateEvents();

    EntityUpdateEvent eue = entityUpdateEvents.get(0);
    String entityId = eue.getEntityId();
    YamlGitSync ygs = get(entityId, eue.getAccountId(), eue.getAppId());

    if (ygs == null) {
      // no git sync found for this entity
      return false;
    }

    File sshKeyPath = GitSyncHelper.getSshKeyPath(ygs.getSshKey(), entityId);
    GitSyncHelper gsh = new GitSyncHelper(ygs.getPassphrase(), sshKeyPath.getAbsolutePath());
    File repoPath = gsh.getTempRepoPath(entityId);
    // clone the repo
    gsh.clone(ygs.getUrl(), repoPath);

    List<GitSyncFile> gitSyncFiles = entityUpdateListEvent.getGitSyncFiles();

    gsh.writeAddCommitPush(ygs, repoPath, gitSyncFiles);
    gsh.cleanupTempFiles(sshKeyPath, repoPath);
    gsh.shutdown();

    return true;
  }

  public boolean handleHomogeneousEntityUpdateList(List<EntityUpdateEvent> entityUpdateEvents) {
    logger.info("*************** handleHomogeneousEntityUpdateList");

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

  public RestResponse processWebhookPost(String accountId, String webhookToken, String rawJson) {
    RestResponse rr = new RestResponse();

    logger.info("********** accountId: " + accountId + ", webhookToken: " + webhookToken);
    // logger.info("********** rawJson: " + rawJson);

    GitSyncWebhook gsw = YamlHelper.verifyWebhookToken(wingsPersistence, accountId, webhookToken);
    String entityId = gsw.getEntityId();

    logger.info("********** entityId: " + entityId);

    if (rawJson.contains("hook_id") && (!rawJson.contains("head_commit") && !rawJson.contains("forced"))) {
      logger.info("********** Looks like a GitHub Webhook initial setup POST.");

      return processInitialGitHubWebhookPost(gsw, rawJson);
    } else {
      if ((rawJson.contains("head_commit") && rawJson.contains("forced")) && !rawJson.contains("hook_id")) {
        logger.info("********** Looks like a GitHub Webhook commit (push) event POST.");

        return processGitHubPushWebhookPost(gsw, rawJson);
      } else {
        logger.info("********** ERROR: GitHub Webhook POST not recognized!");

        rr.addResponseMessage(
            ResponseMessage.Builder.aResponseMessage().withMessage("This webhook payload is not recognized.").build());
      }
    }

    return rr;
  }

  public GitSyncWebhook getWebhook(String entityId, String accountId) {
    GitSyncWebhook gsw = YamlHelper.checkForWebhookToken(wingsPersistence, accountId, entityId);

    if (gsw != null) {
      return gsw;
    } else {
      // create a new GitSyncWebhook, save to Mongo and return it
      String newWebhookToken = CryptoUtil.secureRandAlphaNumString(40);
      gsw = GitSyncWebhook.builder().accountId(accountId).entityId(entityId).webhookToken(newWebhookToken).build();
      return wingsPersistence.saveAndGet(GitSyncWebhook.class, gsw);
    }
  }

  private RestResponse processInitialGitHubWebhookPost(GitSyncWebhook gsw, String rawJson) {
    RestResponse rr = new RestResponse();

    // we should (attempt to) pushDirectory now that the webhook has been setup/initialized (on the GutHub side)
    yamlDirectoryService.pushDirectory(gsw.getAccountId(), false);

    return rr;
  }

  private RestResponse processGitHubPushWebhookPost(GitSyncWebhook gsw, String rawJson) {
    logger.info("************* processGitHubPushWebhookPost");

    RestResponse rr = new RestResponse();

    /*
    Object webhookPost = null;

    try {
      ObjectMapper mapper = new ObjectMapper();
      webhookPost = mapper.readValue(rawJson, Object.class);

      //logger.info("********** webhookPost: " + webhookPost);

    } catch (IOException e) {
      e.printStackTrace();
    }
    */

    // we need to get the Setup (account level) yaml git sync info for this account
    YamlGitSync ygs = get(gsw.getEntityId(), gsw.getAccountId(), GLOBAL_APP_ID);

    if (ygs == null) {
      rr.addResponseMessage(
          ResponseMessage.Builder.aResponseMessage().withMessage("Git Sync info for this webhook not found.").build());
      return rr;
    }

    // if the the SyncMode is GIT_TO_HARNESS or BOTH and enabled == true then we can proceed
    SyncMode syncMode = ygs.getSyncMode();
    if (syncMode != SyncMode.GIT_TO_HARNESS && syncMode != SyncMode.BOTH) {
      // nothing to do
      return rr;
    }

    if (!ygs.isEnabled()) {
      // nothing to do
      return rr;
    }

    //------------------------------
    // we need to use the YamlGitSync info to clone the remote git synced repo
    String entityId = gsw.getEntityId();
    File sshKeyPath = GitSyncHelper.getSshKeyPath(ygs.getSshKey(), entityId);
    GitSyncHelper gsh = new GitSyncHelper(ygs.getPassphrase(), sshKeyPath.getAbsolutePath());
    File repoPath = gsh.getTempRepoPath(entityId);
    // clone the repo
    gsh.clone(ygs.getUrl(), repoPath);

    logger.info("************* repoPath: " + repoPath);
    //------------------------------

    //------------------------------
    // we need to write the current yaml directory to the location where we cloned
    String accountId = gsw.getAccountId();
    EntityUpdateListEvent eule =
        EntityUpdateListEvent.Builder.anEntityUpdateListEvent().withAccountId(accountId).withTreeSync(true).build();

    String setupEntityId = "setup";
    FolderNode top = yamlDirectoryService.getDirectory(accountId, setupEntityId, true);

    // traverse the directory and add all the files
    eule = yamlDirectoryService.traverseDirectory(eule, top, "", SourceType.ENTITY_UPDATE);
    List<GitSyncFile> gitSyncFiles = eule.getGitSyncFiles();

    // gsh.writeAndAdd(repoPath, gitSyncFiles);
    gsh.compareAndSaveChanges(repoPath, gitSyncFiles, yamlResourceService, setupYamlResourceService,
        appYamlResourceService, serviceYamlResourceService);
    //------------------------------

    /*
    gsh.spitOutStatus();

    Git git = gsh.getGit();
    Status status = null;
    try {
      status = git.status().call();
      Set<String> changeSet = status.getChanged();

      // TODO - this is inefficient, but it should work (for now)
      // clone again
      gsh.cleanupTempRepoFiles(repoPath);
      gsh.clone(ygs.getUrl(), repoPath);

      for (String change : changeSet) {
        logger.info("************* change: " + change);

        String yaml = new String(Files.readAllBytes(Paths.get(repoPath + "/" + change)));

        logger.info("************* yaml: " + yaml);

        // for every changed file, we need to the update or save (create) or delete the same way it would be done from
    editing the yaml in the UI
        // NOTE - initially, we will not support deletes, because it would be too easy to blow out test data (dev data)
    accidently!

        // TODO - we have a problem - we don't have any of the required Ids - appId, serviceId, etc. to be able to
    update the yaml!

      }

    } catch (GitAPIException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    */

    gsh.cleanupTempFiles(sshKeyPath, repoPath);
    gsh.shutdown();

    return rr;
  }
}
