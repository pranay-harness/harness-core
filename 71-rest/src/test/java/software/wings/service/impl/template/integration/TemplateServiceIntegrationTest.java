package software.wings.service.impl.template.integration;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.threading.Morpheus.sleep;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.template.TemplateType.SSH;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.LATEST_TAG;
import static software.wings.common.TemplateConstants.TOMCAT_WAR_INSTALL_PATH;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_ACCOUNT;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_FOLDER_DEC;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_FOLDER_NAME;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import migrations.all.SystemTemplateGalleryMigration;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.rules.Integration;
import software.wings.rules.SetupScheduler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.sm.states.HttpState;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Integration
@Ignore
@SetupScheduler
public class TemplateServiceIntegrationTest extends WingsBaseTest {
  @Inject private TemplateFolderService templateFolderService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private SystemTemplateGalleryMigration migration;
  @Inject private TemplateService templateService;
  @Inject private TemplateGalleryService templateGalleryService;

  @Test
  public void shouldMigrateGallery() {
    migration.migrate();
  }

  @Test
  public void shouldLoadTemplateFolders() {
    wingsPersistence.delete(
        wingsPersistence.createQuery(TemplateFolder.class).filter(ACCOUNT_ID_KEY, GLOBAL_ACCOUNT_ID));
    templateFolderService.loadDefaultTemplateFolders();
  }

  @Test
  public void shouldGetTemplateTree() {
    TemplateFolder templateFolder = templateFolderService.getTemplateTree(GLOBAL_ACCOUNT_ID, null, null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(templateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
    assertThat(templateFolder.getKeywords()).contains(TEMPLATE_FOLDER_DEC.toLowerCase());
  }

  @Test
  public void shouldGetTemplateTreeByKeyword() {
    TemplateFolder templateFolder = templateFolderService.getTemplateTree("kmpySmUISimoRrJL6NL73w", "Install", null);
    assertThat(templateFolder).isNotNull();
    assertThat(templateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(templateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
    assertThat(templateFolder.getKeywords()).contains(TEMPLATE_FOLDER_DEC.toLowerCase());
  }

  @Test
  public void shouldCopyHarnessTemplateFolders() {
    TemplateGallery templateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    assertThat(templateGallery).isNotNull();
    List<Account> accounts = accountService.list(aPageRequest().addFilter(APP_ID_KEY, EQ, GLOBAL_APP_ID).build());
    accounts.forEach(account -> {
      wingsPersistence.delete(
          wingsPersistence.createQuery(TemplateFolder.class).filter("accountId", account.getUuid()));
      templateFolderService.copyHarnessTemplateFolders(
          templateGallery.getUuid(), account.getUuid(), account.getAccountName());
    });
  }

  @Test
  public void shouldGetAccountTemplateTree() {
    List<Account> accounts = accountService.list(aPageRequest().addFilter("appId", EQ, GLOBAL_APP_ID).build());
    accounts.forEach(account -> {
      TemplateFolder templateFolder = templateFolderService.getTemplateTree(account.getUuid(), null, null);
      assertThat(templateFolder).isNotNull();
      assertThat(templateFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
      assertThat(templateFolder.getKeywords()).contains(TEMPLATE_FOLDER_NAME.toLowerCase());
      assertThat(templateFolder.getKeywords()).contains(TEMPLATE_FOLDER_DEC.toLowerCase());
    });
  }

  @Test
  public void shouldLoadDefaultCommandTemplates() {
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
  }

  @Test
  public void shouldLoadDefaultCommandTemplatesForAccount() {
    List<Account> accounts = accountService.list(aPageRequest().addFilter("appId", EQ, GLOBAL_APP_ID).build());
    accounts.forEach(account -> {
      wingsPersistence.delete(wingsPersistence.createQuery(Template.class).filter("accountId", account.getUuid()));
      templateService.loadDefaultTemplates(SSH, account.getUuid(), account.getAccountName());
    });
  }

  @Test
  public void shouldLoadTomcatStandardInstallCommand() {
    templateService.loadYaml(SSH, TOMCAT_WAR_INSTALL_PATH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
  }

  @Test
  public void shouldUpdateLinkedEntities() {
    Template template = templateService.get("EtnnNloDR5i3cZoQ1LUW0Q");
    templateService.update(template);
  }

  @Test
  public void shouldUpdateLinkedServiceCommandEntities() {
    Template template = templateService.get("EtnnNloDR5i3cZoQ1LUW0Q");
    templateService.update(template);
  }

  @Test
  public void shouldTestExpression() {
    String content = "${F5_URL}/abdcc/";
    Matcher matcher = ManagerExpressionEvaluator.wingsVariablePattern.matcher(content);
    while (matcher.find()) {
      String name = matcher.group(0);
      assertThat(name).isEqualTo(content);
    }
  }

  @Test
  public void shouldCreateHttpTemplate() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    HttpTemplate httpTemplate = HttpTemplate.builder()
                                    .url("$workflow.variables.F5_URL}/mgmt/tm/${foo}/members")
                                    .assertion("200 ok")
                                    .body("{ \"kind\": ${LB_TYPE}}")
                                    .header("Authorization:${workflow.variables.F5_AUTH_BASE64}")
                                    .build();

    Template template = Template.builder()
                            .templateObject(httpTemplate)
                            .folderId(parentFolder.getUuid())
                            .appId(GLOBAL_APP_ID)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .name("Enable Instance4")
                            .build();
    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getKeywords()).isNotEmpty();
    assertThat(savedTemplate.getKeywords()).contains(LATEST_TAG, template.getName().toLowerCase());
    assertThat(savedTemplate.getVersion()).isEqualTo(1);
    assertThat(savedTemplate.getVariables()).isNotEmpty();
    assertThat(savedTemplate.getVariables())
        .extracting("name")
        .contains("${foo}", "${LB_TYPE}", "${workflow.variables.F5_AUTH_BASE64}");
    HttpState savedHttpTemplate = (HttpState) savedTemplate.getTemplateObject();
    assertThat(savedHttpTemplate).isNotNull();
    assertThat(savedHttpTemplate.getAssertion()).isNotEmpty();
  }

  @Test
  public void shouldLoadDefaultsOnAccountCreation() {
    Account account = accountService.getByAccountName(TEMPLATE_ACCOUNT);
    if (account != null) {
      accountService.delete(account.getUuid());
    }

    Account savedAccount =
        accountService.save(anAccount().withCompanyName(TEMPLATE_ACCOUNT).withAccountName(TEMPLATE_ACCOUNT).build());
    assertThat(savedAccount).isNotNull();
    sleep(Duration.ofSeconds(2));

    TemplateFolder harnessTemplateFolder = templateService.getTemplateTree(
        savedAccount.getUuid(), null, asList(TemplateType.SSH.name(), TemplateType.HTTP.name()));
    assertThat(harnessTemplateFolder).isNotNull();
    assertThat(harnessTemplateFolder.getName()).isEqualTo(TEMPLATE_ACCOUNT);

    PageRequest<Template> pageRequest = aPageRequest()
                                            .addFilter(ACCOUNT_ID_KEY, EQ, savedAccount.getUuid())
                                            .addFilter(APP_ID_KEY, EQ, GLOBAL_APP_ID)
                                            .build();

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

  @Test
  public void shouldDeleteGalleryOnAccountDeletion() {
    String accountId;
    Account account = accountService.getByAccountName(TEMPLATE_ACCOUNT);
    if (account != null) {
      accountId = account.getUuid();
      accountService.delete(account.getUuid());
    } else {
      Account savedAccount =
          accountService.save(anAccount().withCompanyName(TEMPLATE_ACCOUNT).withAccountName(TEMPLATE_ACCOUNT).build());
      assertThat(savedAccount).isNotNull();
      sleep(Duration.ofSeconds(2));
      accountId = savedAccount.getUuid();
      accountService.delete(account.getUuid());
    }
    sleep(Duration.ofSeconds(2));
    TemplateFolder harnessTemplateFolder =
        templateService.getTemplateTree(accountId, null, asList(TemplateType.SSH.name(), TemplateType.HTTP.name()));
    assertThat(harnessTemplateFolder).isNull();

    PageRequest<Template> pageRequest =
        aPageRequest().addFilter(ACCOUNT_ID_KEY, EQ, accountId).addFilter(APP_ID_KEY, EQ, GLOBAL_APP_ID).build();

    List<Template> templates = templateService.list(pageRequest);

    assertThat(templates).isEmpty();
  }
}
