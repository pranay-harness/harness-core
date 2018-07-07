package software.wings.service.impl.template;

import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.CREATED_AT_KEY;
import static software.wings.beans.template.TemplateVersion.INITIAL_VERSION;
import static software.wings.beans.template.TemplateVersion.TEMPLATE_UUID_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.template.TemplateVersion;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateVersionService;

@Singleton
public class TemplateVersionServiceImpl implements TemplateVersionService {
  private static final Logger logger = LoggerFactory.getLogger(TemplateVersionServiceImpl.class);
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
        .order(Sort.descending(CREATED_AT_KEY))
        .get();
  }

  @Override
  public TemplateVersion newTemplateVersion(String accountId, String templateUuid, String templateType,
      String templateName, TemplateVersion.ChangeType changeType) {
    TemplateVersion templateVersion = TemplateVersion.builder()
                                          .accountId(accountId)
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
        templateVersion = wingsPersistence.saveAndGet(TemplateVersion.class, templateVersion);
        done = true;
      } catch (Exception e) {
        logger.warn(String.format("TemplateVersion save failed templateUuid: %s- attemptNo: %s", templateUuid, i), e);
        i++;
      }
    } while (!done && i < 3);

    return templateVersion;
  }
}
