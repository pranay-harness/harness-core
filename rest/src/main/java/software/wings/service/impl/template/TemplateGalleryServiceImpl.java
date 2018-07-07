package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtils.trimList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.template.TemplateGallery.NAME_KEY;
import static software.wings.exception.WingsException.USER;
import static software.wings.utils.Validator.duplicateCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.ListUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateHelper;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateGalleryService;

import java.util.List;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class TemplateGalleryServiceImpl implements TemplateGalleryService {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public PageResponse<TemplateGallery> list(PageRequest<TemplateGallery> pageRequest) {
    return wingsPersistence.query(TemplateGallery.class, pageRequest);
  }

  @Override
  public TemplateGallery save(TemplateGallery templateGallery) {
    templateGallery.setKeywords(getKeywords(templateGallery));
    TemplateGallery finalTemplateGallery = templateGallery;
    return duplicateCheck(()
                              -> wingsPersistence.saveAndGet(TemplateGallery.class, finalTemplateGallery),
        NAME_KEY, templateGallery.getName());
  }

  @Override
  public TemplateGallery get(String accountId, String galleryName) {
    return wingsPersistence.createQuery(TemplateGallery.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .filter(NAME_KEY, galleryName.trim())
        .get();
  }

  @Override
  public TemplateGallery get(String uuid) {
    return wingsPersistence.get(TemplateGallery.class, uuid);
  }

  @Override
  public TemplateGallery update(TemplateGallery templateGallery) {
    TemplateGallery savedGallery = get(templateGallery.getUuid());
    notNullCheck("Template Gallery [" + templateGallery.getName() + "] was deleted", savedGallery, USER);

    Query<TemplateGallery> query =
        wingsPersistence.createQuery(TemplateGallery.class).field(ID_KEY).equal(templateGallery.getUuid());
    UpdateOperations<TemplateGallery> operations = wingsPersistence.createUpdateOperations(TemplateGallery.class);

    List<String> userKeywords = ListUtils.trimStrings(templateGallery.getKeywords());
    if (isNotEmpty(templateGallery.getDescription())) {
      if (isNotEmpty(userKeywords)) {
        userKeywords.remove(savedGallery.getDescription().toLowerCase());
      }
      operations.set("description", templateGallery.getDescription());
    }
    operations.set("keywords", getKeywords(templateGallery));
    wingsPersistence.update(query, operations);
    return get(savedGallery.getUuid());
  }

  private List<String> getKeywords(TemplateGallery templateGallery) {
    List<String> generatedKeywords = trimList(templateGallery.generateKeywords());
    return TemplateHelper.addUserKeyWords(templateGallery.getKeywords(), generatedKeywords);
  }

  @Override
  public void delete(String galleryUuid) {
    wingsPersistence.delete(TemplateGallery.class, galleryUuid);
  }
}
