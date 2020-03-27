package software.wings.service.impl.template;

import static io.harness.exception.WingsException.USER;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.template.TemplateVersion.ChangeType.IMPORTED;
import static software.wings.beans.template.TemplateVersion.INITIAL_VERSION;
import static software.wings.beans.template.TemplateVersion.TEMPLATE_UUID_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.template.TemplateVersion;
import software.wings.beans.template.TemplateVersion.TemplateVersionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateVersionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@Slf4j
public class TemplateVersionServiceImpl implements TemplateVersionService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<TemplateVersion> listTemplateVersions(PageRequest<TemplateVersion> pageRequest) {
    return wingsPersistence.query(TemplateVersion.class, pageRequest);
  }

  @Override
  public List<TemplateVersion> listImportedTemplateVersions(String templateId, String accountId) {
    return wingsPersistence.createQuery(TemplateVersion.class)
        .filter(TemplateVersionKeys.accountId, accountId)
        .filter(TemplateVersionKeys.templateUuid, templateId)
        .filter(TemplateVersionKeys.changeType, IMPORTED)
        .order(Sort.ascending(TemplateVersionKeys.version))
        .asList();
  }

  @Override
  public Map<String, String> listLatestVersionOfImportedTemplates(List<String> templateUuids, String accountId) {
    List<TemplateVersion> templateVersions = wingsPersistence.createQuery(TemplateVersion.class)
                                                 .filter(TemplateVersionKeys.accountId, accountId)
                                                 .field(TemplateVersionKeys.templateUuid)
                                                 .in(templateUuids)
                                                 .filter(TemplateVersionKeys.changeType, IMPORTED)
                                                 .order(Sort.descending(TemplateVersionKeys.version))
                                                 .asList();

    Map<String, String> templateUuidLatestVersionMap = new HashMap<>();
    // Since versions are sorted descending first template will be latest.
    for (TemplateVersion templateVersion : templateVersions) {
      if (!templateUuidLatestVersionMap.containsKey(templateVersion.getTemplateUuid())) {
        templateUuidLatestVersionMap.put(
            templateVersion.getTemplateUuid(), String.valueOf(templateVersion.getVersion()));
      }
    }
    return templateUuidLatestVersionMap;
  }

  @Override
  public TemplateVersion lastTemplateVersion(String accountId, String templateUuid) {
    return wingsPersistence.createQuery(TemplateVersion.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .filter(TEMPLATE_UUID_KEY, templateUuid)
        .order(Sort.descending(TemplateVersionKeys.version))
        .get();
  }

  @Override
  public TemplateVersion newReferencedTemplateVersion(String accountId, String galleryId, String templateUuid,
      String templateType, String templateName, Long version, String versionDetails) {
    TemplateVersion templateVersion = TemplateVersion.builder()
                                          .accountId(accountId)
                                          .galleryId(galleryId)
                                          .templateUuid(templateUuid)
                                          .templateName(templateName)
                                          .templateType(templateType)
                                          .changeType(IMPORTED.name())
                                          .version(version)
                                          .versionDetails(versionDetails)
                                          .build();

    if (!checkIfAlreadyExists(accountId, templateUuid, version)) {
      String templateVersionUuid = wingsPersistence.save(templateVersion);
      return wingsPersistence.get(TemplateVersion.class, templateVersionUuid);
    }
    throw new InvalidRequestException("Template already exists.", USER);
  }

  private boolean checkIfAlreadyExists(String accountId, String templateUuid, Long version) {
    TemplateVersion templateVersion = wingsPersistence.createQuery(TemplateVersion.class)
                                          .filter(TemplateVersionKeys.accountId, accountId)
                                          .filter(TemplateVersionKeys.templateUuid, templateUuid)
                                          .filter(TemplateVersionKeys.version, version)
                                          .get();
    return templateVersion != null;
  }

  @Override
  public TemplateVersion newTemplateVersion(String accountId, String galleryId, String templateUuid,
      String templateType, String templateName, TemplateVersion.ChangeType changeType) {
    TemplateVersion templateVersion = TemplateVersion.builder()
                                          .accountId(accountId)
                                          .galleryId(galleryId)
                                          .templateUuid(templateUuid)
                                          .templateName(templateName)
                                          .templateType(templateType)
                                          .changeType(changeType.name())
                                          .build();
    int i = 0;
    boolean done = false;
    do {
      try {
        TemplateVersion lastTemplateVersion = lastTemplateVersion(accountId, templateUuid);
        if (lastTemplateVersion == null) {
          templateVersion.setVersion(INITIAL_VERSION);
        } else {
          templateVersion.setVersion(lastTemplateVersion.getVersion() + 1);
        }
        wingsPersistence.save(templateVersion);
        done = true;
      } catch (Exception e) {
        logger.warn("TemplateVersion save failed templateUuid: {} - attemptNo: {}", templateUuid, i, e);
        i++;
        // If we exception out then done is still 'false' and we will retry again
        templateVersion.setCreatedAt(0);
      }
    } while (!done && i < 3);

    return templateVersion;
  }
}
