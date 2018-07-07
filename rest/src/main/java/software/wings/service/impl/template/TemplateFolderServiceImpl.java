package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtils.trimStrings;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.KEYWORDS_KEY;
import static software.wings.beans.template.Template.TYPE_KEY;
import static software.wings.beans.template.TemplateFolder.GALLERY_ID_KEY;
import static software.wings.beans.template.TemplateFolder.NAME_KEY;
import static software.wings.beans.template.TemplateFolder.NodeType.FOLDER;
import static software.wings.common.TemplateConstants.GENERIC_COMMANDS;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.HTTP_VERIFICATION;
import static software.wings.common.TemplateConstants.JBOSS_COMMANDS;
import static software.wings.common.TemplateConstants.PATH_DELIMITER;
import static software.wings.common.TemplateConstants.POWER_SHELL_COMMANDS;
import static software.wings.common.TemplateConstants.TOMCAT_COMMANDS;
import static software.wings.exception.WingsException.SRE;
import static software.wings.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.ListUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Base;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateHelper;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class TemplateFolderServiceImpl implements TemplateFolderService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private TemplateGalleryService templateGalleryService;
  @Inject private TemplateService templateService;

  @Override
  public PageResponse<TemplateFolder> list(PageRequest<TemplateFolder> pageRequest) {
    return wingsPersistence.query(TemplateFolder.class, pageRequest);
  }

  @Override
  public TemplateFolder save(TemplateFolder templateFolder) {
    TemplateGallery templateGallery;
    if (isEmpty(templateFolder.getGalleryId())) {
      templateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    } else {
      templateGallery = templateGalleryService.get(templateFolder.getGalleryId());
    }
    notNullCheck("Template Gallery", templateGallery, USER);
    templateFolder.setGalleryId(templateGallery.getUuid());
    if (!isEmpty(templateFolder.getParentId())) {
      TemplateFolder parentFolder = get(templateFolder.getParentId());
      notNullCheck("Parent template folder was deleted", parentFolder);
      String pathId = parentFolder.getPathId() == null
          ? parentFolder.getUuid()
          : parentFolder.getPathId() + PATH_DELIMITER + parentFolder.getUuid();
      templateFolder.setPathId(pathId);
    }
    templateFolder.setKeywords(getKeywords(templateFolder));
    return wingsPersistence.saveAndGet(TemplateFolder.class, templateFolder);
  }

  private List<String> getKeywords(TemplateFolder templateFolder) {
    List<String> generatedKeywords = ListUtils.trimList(templateFolder.generateKeywords());
    return TemplateHelper.addUserKeyWords(templateFolder.getKeywords(), generatedKeywords);
  }

  @Override
  public TemplateFolder update(TemplateFolder templateFolder) {
    TemplateFolder savedTemplateFolder = get(templateFolder.getUuid());
    if (savedTemplateFolder == null) {
      throw new WingsException("Template Folder [" + templateFolder.getName() + "] was deleted", USER);
    }
    Query<TemplateFolder> query =
        wingsPersistence.createQuery(TemplateFolder.class).field(ID_KEY).equal(savedTemplateFolder.getUuid());
    UpdateOperations<TemplateFolder> operations = wingsPersistence.createUpdateOperations(TemplateFolder.class);

    List<String> userKeywords = trimStrings(savedTemplateFolder.getKeywords());

    if (isNotEmpty(savedTemplateFolder.getDescription())) {
      if (isNotEmpty(userKeywords)) {
        userKeywords.remove(savedTemplateFolder.getDescription().toLowerCase());
      }
      operations.set("description", templateFolder.getDescription());
    }
    operations.set("name", templateFolder.getName());
    operations.set("keywords", getKeywords(templateFolder));
    wingsPersistence.update(query, operations);
    return get(savedTemplateFolder.getUuid());
  }

  @Override
  public boolean delete(String templateFolderUuid) {
    // Delete all the templates
    TemplateFolder templateFolder = get(templateFolderUuid);
    if (templateService.deleteByFolder(templateFolder)) {
      // Delete children
      wingsPersistence.delete(wingsPersistence.createQuery(TemplateFolder.class)
                                  .filter(ACCOUNT_ID_KEY, templateFolder.getAccountId())
                                  .field(TemplateFolder.PATH_ID_KEY)
                                  .contains(templateFolderUuid));
      return wingsPersistence.delete(TemplateFolder.class, templateFolderUuid);
    }
    return false;
  }

  @Override
  public TemplateFolder get(String uuid) {
    return wingsPersistence.get(TemplateFolder.class, uuid);
  }

  @Override
  public void loadDefaultTemplateFolders() {
    TemplateGallery templateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    Validator.notNullCheck("Harness Template gallery was deleted", templateGallery, SRE);

    TemplateFolder harnessFolder = save(TemplateFolder.builder()
                                            .appId(GLOBAL_APP_ID)
                                            .accountId(GLOBAL_ACCOUNT_ID)
                                            .galleryId(templateGallery.getUuid())
                                            .name(HARNESS_GALLERY)
                                            .build());
    // Generic commands folder
    save(constructTemplateBuilder(harnessFolder, GENERIC_COMMANDS));

    // Jboss commands folder
    save(constructTemplateBuilder(harnessFolder, JBOSS_COMMANDS));

    // Adding Load Balancer folder
    //    TemplateFolder loadBalancer = save(constructTemplateBuilder(harnessFolder, LOAD_BALANCERS));
    //    save(constructTemplateBuilder(loadBalancer, F5_LOAD_BALANCER, FILE));

    // Power Shell commands folder
    save(constructTemplateBuilder(harnessFolder, POWER_SHELL_COMMANDS));

    // Tomcat Commands Folder
    save(constructTemplateBuilder(harnessFolder, TOMCAT_COMMANDS));

    // Http Verifications
    save(constructTemplateBuilder(harnessFolder, HTTP_VERIFICATION));
  }

  private TemplateFolder constructTemplateBuilder(TemplateFolder parentFolder, String folderName) {
    String pathId = parentFolder.getPathId() == null
        ? parentFolder.getUuid()
        : parentFolder.getPathId() + PATH_DELIMITER + parentFolder.getUuid();
    return TemplateFolder.builder()
        .name(folderName)
        .parentId(parentFolder.getUuid())
        .galleryId(parentFolder.getGalleryId())
        .pathId(pathId)
        .appId(GLOBAL_APP_ID)
        .accountId(GLOBAL_ACCOUNT_ID)
        .build();
  }

  @Override
  public TemplateFolder getTemplateTree(String accountId, String keyword, List<String> templateTypes) {
    Query<TemplateFolder> folderQuery =
        wingsPersistence.createQuery(TemplateFolder.class).filter(ACCOUNT_ID_KEY, accountId);

    Query<Template> templateQuery = wingsPersistence.createQuery(Template.class).filter(ACCOUNT_ID_KEY, accountId);

    if (isNotEmpty(keyword)) {
      folderQuery = folderQuery.field(KEYWORDS_KEY).contains(keyword.toLowerCase());
      templateQuery = templateQuery.field(KEYWORDS_KEY).contains(keyword.toLowerCase());
    }

    List<TemplateFolder> templateFolders = new ArrayList<>();
    boolean contentOnlySearch = false;
    if (isNotEmpty(templateTypes)) {
      templateQuery = templateQuery.field(TYPE_KEY).in(templateTypes);
      contentOnlySearch = true;
    }
    if (!contentOnlySearch) {
      templateFolders = folderQuery.asList();

      List<TemplateFolder> rootTemplateFolders = templateFolders.stream()
                                                     .filter(templateFolder -> isEmpty(templateFolder.getPathId()))
                                                     .collect(Collectors.toList());
      if (isNotEmpty(rootTemplateFolders)) {
        for (TemplateFolder rootTemplateFolder : rootTemplateFolders) {
          templateFolders.addAll(wingsPersistence.createQuery(TemplateFolder.class)
                                     .filter(ACCOUNT_ID_KEY, accountId)
                                     .filter(GALLERY_ID_KEY, rootTemplateFolder.getGalleryId())
                                     .asList());
        }
      }

      List<String> parentUuids =
          templateFolders.stream()
              .filter(templateFolder -> isNotEmpty(templateFolder.getPathId()))
              .flatMap(templateFolder -> Arrays.stream(templateFolder.getPathId().split("/")).distinct())
              .collect(Collectors.toList());
      templateFolders.addAll(getTemplateFolderUuids(accountId, parentUuids));
    }

    List<Template> templates = templateQuery.asList();
    List<String> templateFolderUuids =
        templates.stream()
            .flatMap(template -> Arrays.stream(template.getFolderPathId().split("/")).distinct())
            .collect(Collectors.toList());

    List<TemplateFolder> templateParentFolders = getTemplateFolderUuids(accountId, templateFolderUuids);
    templateFolders.addAll(templateParentFolders);
    Map<String, Long> folderIdChildCountMap = templateFolders.stream().collect(Collectors.toMap(Base::getUuid,
        templateFolder
        -> templates.stream().filter(t -> t.getFolderPathId().contains(templateFolder.getUuid())).count(),
        (a, b) -> b));
    return constructTemplateTree(templateFolders, folderIdChildCountMap);
  }

  private List<TemplateFolder> getTemplateFolderUuids(String accountId, List<String> parentUuids) {
    return wingsPersistence.createQuery(TemplateFolder.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .field("uuid")
        .in(parentUuids)
        .asList();
  }

  private TemplateFolder constructTemplateTree(
      List<TemplateFolder> templateFolders, Map<String, Long> folderIdChildCountMap) {
    Map<String, TemplateFolder> folderMap = new HashMap<>();
    templateFolders.forEach(templateFolder -> folderMap.putIfAbsent(templateFolder.getUuid(), templateFolder));
    folderMap.values().forEach(folder -> {
      folder.setTemplatesCount(folderIdChildCountMap.get(folder.getUuid()));
      if (isNotEmpty(folder.getParentId())) {
        TemplateFolder templateFolder = folderMap.get(folder.getParentId());
        if (templateFolder != null) {
          templateFolder.setNodeType(FOLDER.name());
          templateFolder.addChild(folder);
        } else {
          throw new WingsException("Failed to construct tree from partial list", USER);
        }
      } else {
        folder.setNodeType(FOLDER.name());
      }
    });
    return folderMap.values().stream().filter(folder -> isEmpty(folder.getParentId())).findFirst().orElse(null);
  }

  @Override
  public void copyHarnessTemplateFolders(String accountId) {
    // First Get the Harness template folder
    TemplateFolder harnessTemplateFolder = getTemplateTree(GLOBAL_ACCOUNT_ID, null, null);
    TemplateFolder destinationRootFolder = createTemplateFolder(harnessTemplateFolder, accountId, null);
    createChildren(harnessTemplateFolder, accountId, destinationRootFolder);
  }

  @Override
  public TemplateFolder getByFolderPath(String accountId, String folderPath) {
    String[] folderPaths = folderPath.split("/");
    int beginIndex = folderPath.lastIndexOf('/');
    List<TemplateFolder> templateFolders = getTemplateFolders(accountId, folderPath.substring(beginIndex + 1));
    for (TemplateFolder templateFolder : templateFolders) {
      // Verify the length of the parent folder matches the length of the given folder path
      // Otherwise, ignore
      if (isEmpty(templateFolder.getPathId())) {
        return templateFolder;
      }
      List<String> parentUuids =
          Arrays.stream(templateFolder.getPathId().split("/")).distinct().collect(Collectors.toList());
      if (folderPaths.length - 1 != parentUuids.size()) {
        continue;
      }
      if (matchesPath(accountId, templateFolder, folderPaths, parentUuids)) {
        return templateFolder;
      }
    }
    return null;
  }

  private List<TemplateFolder> getTemplateFolders(String accountId, String folderName) {
    return wingsPersistence.createQuery(TemplateFolder.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .filter("name", folderName)
        .asList();
  }

  private boolean matchesPath(
      String accountId, TemplateFolder templateFolder, String[] folderPaths, List<String> parentUuids) {
    int i = folderPaths.length - 2;
    if (i <= -1) {
      return templateFolder.getName().equals(folderPaths[0]);
    }

    Collections.reverse(parentUuids);

    Map<String, String> uuidNameMap = fetchTemplateFolderNames(accountId, parentUuids);

    for (String parentUuid : parentUuids) {
      if (uuidNameMap.get(parentUuid).equals(folderPaths[i])) {
        i--;
      } else {
        return false;
      }
    }
    return i == -1;
  }

  public Map<String, String> fetchTemplateFolderNames(String accountId, List<String> parentUuids) {
    List<TemplateFolder> templateFolders = wingsPersistence.createQuery(TemplateFolder.class)
                                               .project(NAME_KEY, true)
                                               .filter(ACCOUNT_ID_KEY, accountId)
                                               .field("uuid")
                                               .in(parentUuids)
                                               .asList();

    return templateFolders.stream().collect(Collectors.toMap(TemplateFolder::getUuid, TemplateFolder::getName));
  }

  private void createChildren(TemplateFolder templateFolder, String accountId, TemplateFolder parentFolder) {
    if (templateFolder == null) {
      return;
    }
    if (isEmpty(templateFolder.getChildren())) {
      return;
    }
    templateFolder.getChildren().forEach(sourceFolder -> {
      TemplateFolder destinationFolder = createTemplateFolder(sourceFolder, accountId, parentFolder);
      createChildren(sourceFolder, accountId, destinationFolder);
    });
  }

  private TemplateFolder createTemplateFolder(
      TemplateFolder sourceFolder, String accountId, TemplateFolder parentFolder) {
    TemplateFolder destinationFolder = sourceFolder.cloneInternal();
    destinationFolder.setAccountId(accountId);
    if (parentFolder != null) {
      if (parentFolder.getUuid() != null) {
        destinationFolder.setParentId(parentFolder.getUuid());
      }
      if (parentFolder.getPathId() == null) {
        destinationFolder.setPathId(parentFolder.getUuid());
      } else {
        destinationFolder.setPathId(parentFolder.getPathId() + "/" + parentFolder.getUuid());
      }
    }
    destinationFolder.setKeywords(getKeywords(destinationFolder));
    destinationFolder = wingsPersistence.saveAndGet(TemplateFolder.class, destinationFolder);
    return destinationFolder;
  }
}
