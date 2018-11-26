package software.wings.service.impl.template;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.api.ScriptType.POWERSHELL;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.command.CommandType.INSTALL;
import static software.wings.beans.command.CommandUnitType.DOWNLOAD_ARTIFACT;
import static software.wings.beans.command.DownloadArtifactCommandUnit.Builder.aDownloadArtifactCommandUnit;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.POWER_SHELL_COMMANDS;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_WEBSITE_INSTALL_PATH;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_CUSTOM_KEYWORD;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_DESC;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_GALLERY;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_GALLERY_DESC;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_GALLERY_DESC_CHANGED;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_NAME;
import static software.wings.utils.WingsTestConstants.INVALID_NAME;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter;
import io.harness.exception.WingsException;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;

import java.util.List;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolationException;

public class TemplateGalleryServiceTest extends WingsBaseTest {
  @Inject @InjectMocks protected TemplateGalleryService templateGalleryService;
  @Inject private TemplateService templateService;
  @Inject private TemplateFolderService templateFolderService;

  @Mock private AccountService accountService;

  @Test
  public void shouldSaveTemplateGallery() {
    TemplateGallery savedTemplateGallery = templateGalleryService.save(prepareTemplateGallery());
    assertTemplateGallery(savedTemplateGallery);
  }

  @Test(expected = ConstraintViolationException.class)
  public void shouldNotSaveInvalidNameTemplateGallery() {
    TemplateGallery templateGallery = prepareTemplateGallery();
    templateGallery.setName(INVALID_NAME);
    templateGalleryService.save(templateGallery);
  }

  @Test
  public void shouldUpdateTemplateGallery() {
    TemplateGallery savedTemplateGallery = templateGalleryService.save(prepareTemplateGallery());

    assertTemplateGallery(savedTemplateGallery);

    savedTemplateGallery.getKeywords().add("CV");
    savedTemplateGallery.setDescription(TEMPLATE_GALLERY_DESC_CHANGED);

    TemplateGallery updatedTemplateGallery = templateGalleryService.update(savedTemplateGallery);

    assertThat(updatedTemplateGallery).isNotNull();
    assertThat(updatedTemplateGallery.getKeywords()).contains("cv");
    assertThat(updatedTemplateGallery.getKeywords())
        .contains(TEMPLATE_GALLERY.trim().toLowerCase(), TEMPLATE_GALLERY_DESC_CHANGED.trim().toLowerCase());
  }

  @Test
  public void shouldDeleteTemplateGallery() {
    TemplateGallery savedTemplateGallery = templateGalleryService.save(prepareTemplateGallery());
    assertTemplateGallery(savedTemplateGallery);

    templateGalleryService.delete(savedTemplateGallery.getUuid());

    TemplateGallery deletedGallery = templateGalleryService.get(savedTemplateGallery.getUuid());
    assertThat(deletedGallery).isNull();
  }

  @Test(expected = WingsException.class)
  public void shouldUpdateTemplateGalleryNotExists() {
    TemplateGallery savedTemplateGallery = templateGalleryService.save(prepareTemplateGallery());

    assertTemplateGallery(savedTemplateGallery);

    templateGalleryService.delete(savedTemplateGallery.getUuid());

    templateGalleryService.update(savedTemplateGallery);
  }

  @Test(expected = ConstraintViolationException.class)
  public void shouldNotUpdateInvalidTemplateGallery() {
    TemplateGallery savedTemplateGallery = templateGalleryService.save(prepareTemplateGallery());

    assertTemplateGallery(savedTemplateGallery);

    savedTemplateGallery.setName(INVALID_NAME);

    templateGalleryService.update(savedTemplateGallery);
  }

  @Test
  public void shouldGetTemplateGallery() {
    TemplateGallery savedTemplateGallery = templateGalleryService.save(prepareTemplateGallery());
    assertThat(savedTemplateGallery).isNotNull();
    assertThat(savedTemplateGallery.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);

    TemplateGallery templateGallery = templateGalleryService.get(savedTemplateGallery.getUuid());
    assertTemplateGallery(templateGallery);
  }

  @Test
  public void shouldGetTemplateGalleryByAccount() {
    TemplateGallery savedTemplateGallery = templateGalleryService.save(prepareTemplateGallery());
    assertThat(savedTemplateGallery).isNotNull();
    assertThat(savedTemplateGallery.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);

    TemplateGallery templateGallery = templateGalleryService.getByAccount(savedTemplateGallery.getAccountId());
    assertTemplateGallery(templateGallery);
  }

  @Test
  public void shouldGetTemplateGalleryByName() {
    TemplateGallery savedTemplateGallery = templateGalleryService.save(prepareTemplateGallery());
    assertThat(savedTemplateGallery).isNotNull();
    assertThat(savedTemplateGallery.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);

    TemplateGallery templateGallery =
        templateGalleryService.get(savedTemplateGallery.getAccountId(), savedTemplateGallery.getName());

    assertTemplateGallery(templateGallery);
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
    assertTemplateGallery(templateGallery);
  }

  @Test
  public void shouldLoadHarnessGallery() {
    templateGalleryService.loadHarnessGallery();

    TemplateGallery templateGallery = templateGalleryService.getByAccount(GLOBAL_ACCOUNT_ID);
    assertThat(templateGallery).isNotNull();
    assertThat(templateGallery.getName()).isEqualTo(HARNESS_GALLERY);
    assertThat(templateGallery.isGlobal()).isTrue();
    assertThat(templateGallery.getReferencedGalleryId()).isNull();

    TemplateFolder harnessTemplateFolder = templateService.getTemplateTree(
        GLOBAL_ACCOUNT_ID, null, asList(TemplateType.SSH.name(), TemplateType.HTTP.name()));
    assertThat(harnessTemplateFolder).isNotNull();
    assertThat(harnessTemplateFolder.getName()).isEqualTo(HARNESS_GALLERY);

    PageRequest<Template> pageRequest = aPageRequest().addFilter("appId", EQ, GLOBAL_APP_ID).build();
    assertTemplates(pageRequest);
  }

  @Test
  public void shouldSaveHarnessGallery() {
    TemplateGallery harnessGallery = templateGalleryService.saveHarnessGallery();
    assertThat(harnessGallery).isNotNull();
    assertThat(harnessGallery.isGlobal()).isTrue();
    assertThat(harnessGallery.getReferencedGalleryId()).isNull();
  }

  @Test
  public void shouldCopyHarnessTemplates() {
    templateGalleryService.loadHarnessGallery();

    when(accountService.listAllAccounts())
        .thenReturn(asList(Account.Builder.anAccount().withUuid(ACCOUNT_ID).withAccountName(ACCOUNT_NAME).build()));

    templateGalleryService.copyHarnessTemplates();

    assertAccountGallery();
  }

  @Test
  public void shouldDeleteByAccountId() {
    templateGalleryService.loadHarnessGallery();

    when(accountService.listAllAccounts())
        .thenReturn(asList(Account.Builder.anAccount().withUuid(ACCOUNT_ID).withAccountName(ACCOUNT_NAME).build()));

    templateGalleryService.copyHarnessTemplates();
    assertAccountGallery();

    templateGalleryService.deleteByAccountId(ACCOUNT_ID);
    assertThat(templateGalleryService.getByAccount(ACCOUNT_ID)).isNull();
  }

  @Test
  public void shouldCopyHarnessTemplatesToAccount() {
    templateGalleryService.loadHarnessGallery();
    templateGalleryService.copyHarnessTemplatesToAccount(ACCOUNT_ID, ACCOUNT_NAME);
    assertAccountGallery();
  }

  private void assertAccountGallery() {
    TemplateFolder harnessTemplateFolder =
        templateService.getTemplateTree(ACCOUNT_ID, null, asList(TemplateType.SSH.name(), TemplateType.HTTP.name()));
    assertThat(harnessTemplateFolder).isNotNull();
    assertThat(harnessTemplateFolder.getName()).isEqualTo(ACCOUNT_NAME);

    PageRequest<Template> pageRequest =
        aPageRequest().addFilter(ACCOUNT_ID_KEY, EQ, ACCOUNT_ID).addFilter(APP_ID_KEY, EQ, GLOBAL_APP_ID).build();
    assertTemplates(pageRequest);
  }

  private void assertTemplates(PageRequest<Template> pageRequest) {
    List<Template> templates = templateService.list(pageRequest);

    assertThat(templates).isNotEmpty();
    assertThat(templates.stream()
                   .filter(template1 -> template1.getType().equals(TemplateType.SSH.name()))
                   .collect(Collectors.toList()))
        .isNotEmpty();

    assertThat(templates.stream()
                   .filter(template1 -> template1.getType().equals(TemplateType.HTTP.name()))
                   .collect(Collectors.toList()))
        .isNotEmpty();
  }

  private TemplateGallery prepareTemplateGallery() {
    return TemplateGallery.builder()
        .name(TEMPLATE_GALLERY)
        .accountId(ACCOUNT_ID)
        .description(TEMPLATE_GALLERY_DESC)
        .appId(GLOBAL_APP_ID)
        .keywords(asList("CD"))
        .build();
  }

  private void assertTemplateGallery(TemplateGallery templateGallery) {
    assertThat(templateGallery).isNotNull().extracting("uuid").isNotEmpty();
    assertThat(templateGallery.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(templateGallery.getKeywords()).contains("cd");
    assertThat(templateGallery.getKeywords()).contains(TEMPLATE_GALLERY.trim().toLowerCase());
    assertThat(templateGallery.getKeywords()).contains(TEMPLATE_GALLERY_DESC.trim().toLowerCase());

    TemplateGallery harnessGallery = templateGalleryService.get(ACCOUNT_ID, templateGallery.getName());
    assertThat(harnessGallery).isNotNull();
    assertThat(templateGallery.getReferencedGalleryId()).isNull();
  }

  @Test
  public void shouldCopyHarnessTemplateFromGalleryToAccounts() {
    templateGalleryService.loadHarnessGallery();
    templateGalleryService.copyHarnessTemplatesToAccount(ACCOUNT_ID, ACCOUNT_NAME);
    templateService.loadYaml(
        TemplateType.SSH, POWER_SHELL_IIS_WEBSITE_INSTALL_PATH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    when(accountService.listAllAccounts())
        .thenReturn(asList(Account.Builder.anAccount().withUuid(ACCOUNT_ID).withAccountName(ACCOUNT_NAME).build()));

    templateGalleryService.copyHarnessTemplateFromGalleryToAccounts(
        POWER_SHELL_COMMANDS, TemplateType.SSH, "Install IIS Website", POWER_SHELL_IIS_WEBSITE_INSTALL_PATH);
    Template createdTemplate = templateService.fetchTemplateByKeyword(ACCOUNT_ID, "iiswebsite");
    assertThat(createdTemplate).isNotNull();
  }

  @Test
  public void shouldCopyNewVersionFromGlobalToAllAccounts() {
    templateGalleryService.loadHarnessGallery();
    templateGalleryService.copyHarnessTemplatesToAccount(ACCOUNT_ID, ACCOUNT_NAME);

    //    Template template = templateService.fetchTemplateByKeyword(GLOBAL_ACCOUNT_ID, "iis");
    SshCommandTemplate sshCommandTemplate = SshCommandTemplate.builder()
                                                .commandType(INSTALL)
                                                .commandUnits(asList(aDownloadArtifactCommandUnit()
                                                                         .withName("Download Artifact")
                                                                         .withCommandPath("${DownloadDirectory}")
                                                                         .withScriptType(POWERSHELL)
                                                                         .withCommandUnitType(DOWNLOAD_ARTIFACT)
                                                                         .build()))
                                                .build();
    Template template = Template.builder()
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .appId(GLOBAL_APP_ID)
                            .gallery(HARNESS_GALLERY)
                            .name("Install")
                            .type("SSH")
                            .description(TEMPLATE_DESC)
                            .folderPath("Harness/Power Shell Commands")
                            .keywords(asList(TEMPLATE_CUSTOM_KEYWORD))
                            .templateObject(sshCommandTemplate)
                            .build();

    when(accountService.listAllAccounts())
        .thenReturn(asList(Account.Builder.anAccount().withUuid(ACCOUNT_ID).withAccountName(ACCOUNT_NAME).build()));

    templateGalleryService.copyNewVersionFromGlobalToAllAccounts(template, "iis");

    Template template1 = templateService.fetchTemplateByKeyword(ACCOUNT_ID, "iis");
    assertThat(template1).isNotNull();
    assertThat(template1.getVersion()).isEqualTo(2L);
  }
}