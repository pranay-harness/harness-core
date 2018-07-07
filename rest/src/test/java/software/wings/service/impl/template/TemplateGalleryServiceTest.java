package software.wings.service.impl.template;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_GALLERY_DESC;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_GALLERY_DESC_CHANGED;

import org.junit.Test;
import software.wings.beans.SearchFilter;
import software.wings.beans.template.TemplateGallery;
import software.wings.dl.PageRequest;
import software.wings.exception.WingsException;

import java.util.List;

public class TemplateGalleryServiceTest extends TemplateBaseTest {
  @Test
  public void shouldSaveTemplateGallery() {
    TemplateGallery savedTemplateGallery = templateGalleryService.save(prepareTemplateGallery());
    assertThat(savedTemplateGallery).isNotNull();
    assertThat(savedTemplateGallery.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplateGallery.getKeywords())
        .contains(
            "cd", savedTemplateGallery.getName().toLowerCase(), savedTemplateGallery.getDescription().toLowerCase());
    assertThat(savedTemplateGallery.getKeywords()).contains(HARNESS_GALLERY.trim().toLowerCase());
  }

  @Test
  public void shouldUpdateTemplateGallery() {
    TemplateGallery savedTemplateGallery = templateGalleryService.save(prepareTemplateGallery());

    assertThat(savedTemplateGallery).isNotNull();
    assertThat(savedTemplateGallery.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplateGallery.getKeywords()).contains("cd");
    assertThat(savedTemplateGallery.getKeywords()).contains(HARNESS_GALLERY.trim().toLowerCase());
    assertThat(savedTemplateGallery.getKeywords()).contains(TEMPLATE_GALLERY_DESC.trim().toLowerCase());

    savedTemplateGallery.getKeywords().add("CV");
    savedTemplateGallery.setDescription(TEMPLATE_GALLERY_DESC_CHANGED);

    TemplateGallery updatedTemplateGallery = templateGalleryService.update(savedTemplateGallery);

    assertThat(updatedTemplateGallery).isNotNull();
    assertThat(updatedTemplateGallery.getKeywords()).contains("cv");
    assertThat(updatedTemplateGallery.getKeywords())
        .contains(HARNESS_GALLERY.trim().toLowerCase(), TEMPLATE_GALLERY_DESC_CHANGED.trim().toLowerCase());
  }

  @Test
  public void shouldDeleteTemplateGallery() {
    TemplateGallery savedTemplateGallery = templateGalleryService.save(prepareTemplateGallery());

    assertThat(savedTemplateGallery).isNotNull().extracting("uuid").isNotEmpty();
    assertThat(savedTemplateGallery.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplateGallery.getKeywords()).contains("cd");
    assertThat(savedTemplateGallery.getKeywords()).contains(HARNESS_GALLERY.trim().toLowerCase());

    templateGalleryService.delete(savedTemplateGallery.getUuid());

    TemplateGallery deletedGallery = templateGalleryService.get(savedTemplateGallery.getUuid());
    assertThat(deletedGallery).isNull();
  }

  @Test(expected = WingsException.class)
  public void shouldUpdateTemplateGalleryNotExists() {
    TemplateGallery savedTemplateGallery = templateGalleryService.save(prepareTemplateGallery());

    assertThat(savedTemplateGallery).isNotNull().extracting("uuid").isNotEmpty();
    assertThat(savedTemplateGallery.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplateGallery.getKeywords()).contains("cd");
    assertThat(savedTemplateGallery.getKeywords()).contains(HARNESS_GALLERY.trim().toLowerCase());

    templateGalleryService.delete(savedTemplateGallery.getUuid());

    templateGalleryService.update(savedTemplateGallery);
  }

  @Test
  public void shouldGetTemplateGallery() {
    TemplateGallery savedTemplateGallery = templateGalleryService.save(prepareTemplateGallery());
    assertThat(savedTemplateGallery).isNotNull();
    assertThat(savedTemplateGallery.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);

    TemplateGallery templateGallery = templateGalleryService.get(savedTemplateGallery.getUuid());
    assertThat(templateGallery).isNotNull();
    assertThat(templateGallery.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(templateGallery.getKeywords()).contains("cd");
    assertThat(templateGallery.getKeywords()).contains(HARNESS_GALLERY.trim().toLowerCase());
  }

  @Test
  public void shouldGetTemplateGalleryByName() {
    TemplateGallery savedTemplateGallery = templateGalleryService.save(prepareTemplateGallery());
    assertThat(savedTemplateGallery).isNotNull();
    assertThat(savedTemplateGallery.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);

    TemplateGallery templateGallery =
        templateGalleryService.get(savedTemplateGallery.getAccountId(), savedTemplateGallery.getName());

    assertThat(templateGallery).isNotNull();
    assertThat(templateGallery.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(templateGallery.getKeywords()).contains("cd");
    assertThat(templateGallery.getKeywords()).contains(HARNESS_GALLERY.trim().toLowerCase());
  }

  @Test
  public void shouldListTemplateGalleries() {
    TemplateGallery savedTemplateGallery = templateGalleryService.save(prepareTemplateGallery());
    assertThat(savedTemplateGallery).isNotNull();
    assertThat(savedTemplateGallery.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);

    PageRequest<TemplateGallery> pageRequest =
        aPageRequest().addFilter("appId", SearchFilter.Operator.EQ, GLOBAL_APP_ID).build();
    List<TemplateGallery> templateGalleries = templateGalleryService.list(pageRequest);

    assertThat(templateGalleries).isNotEmpty();
    TemplateGallery templateGallery = templateGalleries.stream().findFirst().get();
    assertThat(templateGallery).isNotNull();
    assertThat(templateGallery.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(templateGallery.getKeywords()).contains("cd");
    assertThat(templateGallery.getKeywords()).contains(HARNESS_GALLERY.trim().toLowerCase());
  }
}