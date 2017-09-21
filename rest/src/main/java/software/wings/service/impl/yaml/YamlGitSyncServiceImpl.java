package software.wings.service.impl.yaml;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.utils.Validator;
import software.wings.yaml.gitSync.EntityUpdateEvent;
import software.wings.yaml.gitSync.EntityUpdateEvent.CrudType;
import software.wings.yaml.gitSync.GitSyncHelper;
import software.wings.yaml.gitSync.YamlGitSync;
import software.wings.yaml.gitSync.YamlGitSync.Type;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

public class YamlGitSyncServiceImpl implements YamlGitSyncService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;

  /**
   * Gets the yaml git sync info by uuid
   *
   * @param uuid the uuid
   * @return the rest response
   */
  public YamlGitSync getByUuid(String uuid) {
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
   * Gets the yaml git sync info by object type and entitytId
   *
   * @param type the object type
   * @param entityId the uuid of the entity
   * @param accountId the account id
   * @return the rest response
   */
  public YamlGitSync get(Type type, String entityId, @NotEmpty String accountId) {
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
  public boolean exist(@NotEmpty Type type, @NotEmpty String entityId, @NotEmpty String accountId) {
    return wingsPersistence.createQuery(YamlGitSync.class)
               .field("accountId")
               .equal(accountId)
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
  public YamlGitSync save(String accountId, YamlGitSync ygs) {
    Validator.notNullCheck("accountId", ygs.getAccountId());

    // check if it already exists
    if (exist(ygs.getType(), ygs.getEntityId(), accountId)) {
      // do update instead
      return update(ygs.getEntityId(), accountId, ygs);
    }

    YamlGitSync yamlGitSync = wingsPersistence.saveAndGet(YamlGitSync.class, ygs);

    return getByUuid(yamlGitSync.getUuid());
  }

  /**
   * Updates the yaml git sync info by object type and entitytId (uuid)
   *
   * @param entityId the uuid of the entity
   * @param accountId the account id
   * @param ygs the yamlGitSync info
   * @return the rest response
   */
  public YamlGitSync update(String entityId, String accountId, YamlGitSync ygs) {
    // check if it already exists
    if (exist(ygs.getType(), ygs.getEntityId(), accountId)) {
      YamlGitSync yamlGitSync = get(ygs.getType(), ygs.getEntityId(), accountId);

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
      return wingsPersistence.get(YamlGitSync.class, yamlGitSync.getUuid());
    }

    return null;
  }

  public boolean handleEntityUpdateEvent(EntityUpdateEvent entityUpdateEvent) {
    logger.info("*************** handleEntityUpdateEvent: " + entityUpdateEvent);

    String entityId = entityUpdateEvent.getEntityId();
    CrudType crudType = entityUpdateEvent.getCrudType();
    Class klass = entityUpdateEvent.getKlass();

    if (entityId == null || entityId.isEmpty()) {
      logger.info("ERROR: EntityUpdateEvent entityId is missing!");
      return false;
    }

    if (crudType == null) {
      logger.info("ERROR: EntityUpdateEvent crudType is missing!");
      return false;
    }

    if (klass == null) {
      logger.info("ERROR: EntityUpdateEvent class is missing!");
      return false;
    }

    switch (crudType) {
      case CREATE:
        // TODO - needs implementation!
        break;
      case UPDATE:
        YamlGitSync ygs = get(entityId);

        logger.info("*************** ygs: " + ygs);

        // logger.info("*************** ygs.getSshKey(): " + ygs.getSshKey());

        //---------------------
        // TODO - annoying that we need to write the sshKey to a file, because the addIdentity method in
        // createDefaultJSch of the CustomJschConfigSessionFactory requires a path and won't take the key directly!

        File sshKeyPath = null;

        try {
          // Path keyDir = Files.createTempDirectory("sync-repo-keys");
          // sshKeyPath = File.createTempFile(entityId, "", keyDir.toFile());
          sshKeyPath = File.createTempFile("sync-keys_" + entityId, "");

          Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
          perms.add(PosixFilePermission.OWNER_READ);
          perms.add(PosixFilePermission.OWNER_WRITE);
          Files.setPosixFilePermissions(Paths.get(sshKeyPath.getAbsolutePath()), perms);

          // sshKeyPath.delete();
          // sshKeyPath.mkdirs();

          FileWriter fw = new FileWriter(sshKeyPath);
          BufferedWriter bw = new BufferedWriter(fw);
          bw.write(ygs.getSshKey());

          if (bw != null) {
            bw.close();
          }

          if (fw != null) {
            fw.close();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        //---------------------

        GitSyncHelper gsh = new GitSyncHelper(ygs.getPassphrase(), sshKeyPath.getAbsolutePath());

        try {
          //---------------------
          // Path repoDir = Files.createTempDirectory("sync-repos");
          // File repoPath = File.createTempFile("sync-repos_" + entityId, "", repoDir.toFile());
          File repoPath = File.createTempFile("sync-repos_" + entityId, "");

          repoPath.delete();
          repoPath.mkdirs();

          // prints absolute path
          logger.info("Absolute path: " + repoPath.getAbsolutePath());
          //---------------------

          Git git = gsh.clone(ygs.getUrl(), repoPath);

          logger.info("*************** klass.getCanonicalName(): " + klass.getCanonicalName());

          switch (klass.getCanonicalName()) {}

          //---------------------
          // Create a new file and add it to the index
          String fileName = "test_file1.txt";

          File newFile = new File(repoPath, fileName);
          newFile.createNewFile();
          FileWriter writer = new FileWriter(newFile);

          String timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());

          writer.write("Test data"
              + "\n"
              + "test data2"
              + "\n"
              + "Last Commit: " + timestamp + "\n");
          writer.close();
          //---------------------

          try {
            DirCache dirCache = git.add().addFilepattern(fileName).call();
          } catch (GitAPIException e) {
            e.printStackTrace();
          }

          RevCommit rev = gsh.commit("bsollish", "bob@harness.io", "My first test commit");

          System.out.println(rev.toString());

          Iterable<PushResult> pushResults = gsh.push("origin");

          // need to clean up TEMP files
          sshKeyPath.delete();
          repoPath.delete();

        } catch (IOException e) {
          e.printStackTrace();
        }

        break;
      case DELETE:
        // TODO - needs implementation!
        break;
      default:
        // do nothing
    }

    return false;
  }
}
