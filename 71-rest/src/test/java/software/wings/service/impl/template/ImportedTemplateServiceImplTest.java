package software.wings.service.impl.template;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.commandlibrary.api.dto.CommandDTO;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.api.commandlibrary.EnrichedCommandVersionDTO;
import software.wings.beans.template.ImportedTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateGallery.GalleryKey;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.beans.template.dto.HarnessImportedTemplateDetails;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.service.intfc.template.TemplateVersionService;

public class ImportedTemplateServiceImplTest extends TemplateBaseTestHelper {
  @Inject private TemplateGalleryService templateGalleryService;
  //    @Inject private WingsPersistence wingsPersistence;
  @Inject TemplateService templateService;
  @Inject TemplateFolderService templateFolderService;
  @Spy @Inject private ImportedTemplateServiceImpl importedTemplateService;
  @Inject TemplateVersionService templateVersionService;

  private String COMMAND_NAME = "commandName";
  private String COMMAND_ID = "commandId";
  private String COMMAND_STORE_ID = "commandStoreId";

  public void mockItems() {
    MockitoAnnotations.initMocks(this);
    doReturn(CommandDTO.builder().commandStoreId(COMMAND_STORE_ID).id(COMMAND_ID).name(COMMAND_NAME).build())
        .when(importedTemplateService)
        .downloadAndGetCommandDTO(anyString(), anyString());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSaveWhenCommandVersionAlreadyExist() {
    mockItems();
    String version = "1.2";
    doReturn(makeCommandVersionDTO(version))
        .when(importedTemplateService)
        .downloadAndGetCommandVersionDTO(anyString(), anyString(), anyString());

    saveImportedTemplate(COMMAND_NAME, "1.2");

    assertThatThrownBy(()
                           -> importedTemplateService.getAndSaveImportedTemplate(
                               version, COMMAND_ID, COMMAND_STORE_ID, GLOBAL_ACCOUNT_ID));
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSaveWhenDifferentVersionOfCommandExists() {
    mockItems();
    // Different version saved.
    Template savedTemplate = saveImportedTemplate(COMMAND_NAME, "1.1");
    String version = "2.2";
    doReturn(makeCommandVersionDTO(version))
        .when(importedTemplateService)
        .downloadAndGetCommandVersionDTO(anyString(), anyString(), anyString());

    Template updatedTemplate =
        importedTemplateService.getAndSaveImportedTemplate(version, COMMAND_ID, COMMAND_STORE_ID, GLOBAL_ACCOUNT_ID);

    assertThat(updatedTemplate).isNotNull();
    assertThat(updatedTemplate.getUuid()).isEqualTo(savedTemplate.getUuid());
    assertThat(updatedTemplate.getImportedTemplateDetails()).isInstanceOf(HarnessImportedTemplateDetails.class);
    assertThat(((HarnessImportedTemplateDetails) updatedTemplate.getImportedTemplateDetails()).getImportedCommandId())
        .isEqualTo(COMMAND_ID);
    assertThat(
        ((HarnessImportedTemplateDetails) updatedTemplate.getImportedTemplateDetails()).getImportedCommandVersion())
        .isEqualTo("2.2");
    assertThat(
        ((HarnessImportedTemplateDetails) updatedTemplate.getImportedTemplateDetails()).getImportedCommandStoreId())
        .isEqualTo(COMMAND_STORE_ID);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSaveWhenFirstVersionOfCommandIsDownloaded() {
    mockItems();
    String version = "1.2";
    doReturn(makeCommandVersionDTO(version))
        .when(importedTemplateService)
        .downloadAndGetCommandVersionDTO(anyString(), anyString(), anyString());

    assertThat(
        importedTemplateService.getAndSaveImportedTemplate(version, COMMAND_ID, COMMAND_STORE_ID, GLOBAL_ACCOUNT_ID))
        .isNotNull();
  }

  private Template saveImportedTemplate(String templateName, String version) {
    HarnessImportedTemplateDetails harnessImportedTemplateDetails = HarnessImportedTemplateDetails.builder()
                                                                        .importedCommandId(COMMAND_ID)
                                                                        .importedCommandStoreId(COMMAND_STORE_ID)
                                                                        .importedCommandVersion(version)
                                                                        .build();
    TemplateGallery templateGallery =
        templateGalleryService.getByAccount(GLOBAL_ACCOUNT_ID, GalleryKey.HARNESS_COMMAND_LIBRARY_GALLERY);

    Template template = Template.builder()
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .appId(GLOBAL_APP_ID)
                            .name(templateName)
                            .templateObject(HttpTemplate.builder().build())
                            .galleryId(templateGallery.getUuid())
                            .importedTemplateDetails(harnessImportedTemplateDetails)
                            .build();
    String templateId = templateService.saveReferenceTemplate(template).getUuid();
    ImportedTemplate importedTemplate = ImportedTemplate.builder()
                                            .templateId(templateId)
                                            .name(COMMAND_NAME)
                                            .appId(GLOBAL_APP_ID)
                                            .accountId(GLOBAL_ACCOUNT_ID)
                                            .commandId(COMMAND_ID)
                                            .commandStoreId(COMMAND_STORE_ID)
                                            .build();
    wingsPersistence.save(importedTemplate);
    return template;
  }

  private EnrichedCommandVersionDTO makeCommandVersionDTO(String version) {
    return EnrichedCommandVersionDTO.builder()
        .commandId(COMMAND_ID)
        .commandStoreId(COMMAND_STORE_ID)
        .description("desc")
        .templateObject(HttpTemplate.builder().build())
        .version(version)
        .build();
  }
}
