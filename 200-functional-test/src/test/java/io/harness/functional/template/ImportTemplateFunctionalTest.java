/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.template;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.template.Template;
import software.wings.service.intfc.template.TemplateService;

import com.google.inject.Inject;
import io.restassured.http.ContentType;
import javax.ws.rs.core.GenericType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ImportTemplateFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private AccountGenerator accountGenerator;
  @Inject private CommandGenerator commandGenerator;
  @Inject private TemplateService templateService;

  final Randomizer.Seed seed = new Randomizer.Seed(0);
  OwnerManager.Owners owners;
  Application application;
  Account account;
  final String version = "1.0";
  final String commandName = "commandName";
  final String commandStoreName = "commandStoreName";

  @Before
  public void setUp() {
    owners = ownerManager.create();
    account = owners.obtainAccount();
    if (account == null) {
      account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    }
    commandGenerator.ensureCommandVersionEntity(seed, owners, "1.0", commandName, commandStoreName);
  }

  @Test
  @Owner(developers = ABHINAV, intermittent = true)
  @Category(FunctionalTests.class)
  public void testDownloadTemplate() {
    GenericType<RestResponse<Template>> templateType = new GenericType<RestResponse<Template>>() {

    };

    RestResponse<Template> savedTemplateResponse = downloadCommand(templateType, bearerToken);
    Template template = savedTemplateResponse.getResource();
    assertThat(template).isNotNull();
    assertThat(template.getImportedTemplateDetails()).isNotNull();
    templateService.delete(account.getUuid(), template.getUuid());
  }

  private RestResponse<Template> downloadCommand(GenericType<RestResponse<Template>> templateType, String bearerToken) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", account.getUuid())
        .pathParam("commandName", commandName)
        .pathParam("commandStoreName", commandStoreName)
        .pathParam("version", version)
        .contentType(ContentType.JSON)
        .post("/command-library/command-stores/{commandStoreName}/commands/{commandName}/versions/{version}")
        .as(templateType.getType());
  }

  @Override
  protected boolean needCommandLibraryService() {
    return true;
  }
}
