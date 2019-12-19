package software.wings.service.impl.template;

import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.template.TemplateVersion.INITIAL_VERSION;
import static software.wings.beans.template.TemplateVersion.TEMPLATE_UUID_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.template.TemplateVersion;
import software.wings.beans.template.TemplateVersion.TemplateVersionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateVersionService;

@Singleton
@Slf4j
public class TemplateVersionServiceImpl implements TemplateVersionService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<TemplateVersion> listTemplateVersions(PageRequest<TemplateVersion> pageRequest) {
    return wingsPersistence.query(TemplateVersion.class, pageRequest);
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
