package software.wings.service.impl.template;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.commandlibrary.api.dto.CommandDTO;
import io.harness.commandlibrary.api.dto.CommandVersionDTO;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.beans.template.ImportedCommandTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.command.HttpTemplate;
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
  private String REF_TEMPLATE_ID = "refTemplateId";
  private String REF_TEMPLATE_STORE = "refTemplateStore";

  public void mockItems() {
    MockitoAnnotations.initMocks(this);
    doReturn(CommandDTO.builder().commandStoreId(REF_TEMPLATE_STORE).id(REF_TEMPLATE_ID).name(COMMAND_NAME).build())
        .when(importedTemplateService)
        .downloadAndGetCommandDTO(anyString(), anyString());
    doReturn(CommandVersionDTO.newBuilder().build())
        .when(importedTemplateService)
        .downloadAndGetCommandVersionDTO(anyString(), anyString());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSaveWhenCommandVersionAlreadyExist() {
    mockItems();
    Template template = Template.builder()
                            .version(Long.valueOf("1"))
                            .isImported(true)
                            .referencedTemplateId(REF_TEMPLATE_ID)
                            .referencedTemplateStoreId(REF_TEMPLATE_STORE)
                            .referencedTemplateVersion(1L)
                            .name(COMMAND_NAME)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .appId(GLOBAL_APP_ID)
                            .version(1)
                            .templateObject(HttpTemplate.builder().build())
                            .build();
    doReturn(template).when(importedTemplateService).createTemplateFromCommandVersionDTO(any(), any());
    saveImportedTemplate(COMMAND_NAME, 1L);

    assertThatThrownBy(()
                           -> importedTemplateService.getAndSaveImportedTemplate(
                               "token", "1", REF_TEMPLATE_ID, REF_TEMPLATE_STORE, GLOBAL_ACCOUNT_ID));
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSaveWhenDifferentVersionOfCommandExist() {
    mockItems();
    Template template = Template.builder()
                            .version(Long.valueOf("2"))
                            .isImported(true)
                            .referencedTemplateId(REF_TEMPLATE_ID)
                            .referencedTemplateStoreId(REF_TEMPLATE_STORE)
                            .referencedTemplateVersion(2L)
                            .name(COMMAND_NAME)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .appId(GLOBAL_APP_ID)
                            .version(2)
                            .templateObject(HttpTemplate.builder().build())
                            .build();
    doReturn(template).when(importedTemplateService).createTemplateFromCommandVersionDTO(any(), any());
    Template savedTemplate = saveImportedTemplate(COMMAND_NAME, 1L);

    Template updatedTemplate = importedTemplateService.getAndSaveImportedTemplate(
        "token", "2", REF_TEMPLATE_ID, REF_TEMPLATE_STORE, GLOBAL_ACCOUNT_ID);
    assertThat(updatedTemplate).isNotNull();
    assertThat(updatedTemplate.getUuid()).isEqualTo(savedTemplate.getUuid());
    assertThat(updatedTemplate.isImported()).isEqualTo(true);
    assertThat(updatedTemplate.getReferencedTemplateVersion()).isEqualTo(2L);
    assertThat(updatedTemplate.getReferencedTemplateId()).isEqualTo(REF_TEMPLATE_ID);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSaveWheFirstVersionOfCommandIsDownloaded() {
    mockItems();
    Template template = Template.builder()
                            .version(Long.valueOf("1"))
                            .isImported(true)
                            .referencedTemplateId(REF_TEMPLATE_ID)
                            .referencedTemplateStoreId(REF_TEMPLATE_STORE)
                            .referencedTemplateVersion(1L)
                            .name(COMMAND_NAME)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .appId(GLOBAL_APP_ID)
                            .version(1)
                            .templateObject(HttpTemplate.builder().build())
                            .build();
    doReturn(template).when(importedTemplateService).createTemplateFromCommandVersionDTO(any(), any());
    assertThat(importedTemplateService.getAndSaveImportedTemplate(
                   "token", "1", REF_TEMPLATE_ID, REF_TEMPLATE_STORE, GLOBAL_ACCOUNT_ID))
        .isNotNull();
  }

  private Template saveImportedTemplate(String templateName, Long version) {
    Template template = Template.builder()
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .appId(GLOBAL_APP_ID)
                            .name(templateName)
                            .templateObject(HttpTemplate.builder().build())
                            .version(version)
                            .referencedTemplateId(REF_TEMPLATE_ID)
                            .referencedTemplateStoreId(REF_TEMPLATE_STORE)
                            .referencedTemplateVersion(version)
                            .build();
    String templateId = templateService.saveReferenceTemplate(template).getUuid();
    ImportedCommandTemplate importedCommandTemplate = ImportedCommandTemplate.builder()
                                                          .templateId(templateId)
                                                          .name(COMMAND_NAME)
                                                          .appId(GLOBAL_APP_ID)
                                                          .accountId(GLOBAL_ACCOUNT_ID)
                                                          .commandId(REF_TEMPLATE_ID)
                                                          .commandStoreId(REF_TEMPLATE_STORE)
                                                          .build();
    wingsPersistence.save(importedCommandTemplate);
    return template;
  }
}
