/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.rule.OwnerRule.AADITI;

import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_NAME;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Event;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.yaml.YamlPushService;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class EnvironmentServiceDBTest extends WingsBaseTest {
  @Inject @InjectMocks private HPersistence persistence;
  @Inject @InjectMocks private EnvironmentService environmentService;
  @Mock private YamlPushService yamlPushService;

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testDeleteConfigMapYamlByServiceTemplateId() {
    Service service = Service.builder().name(SERVICE_NAME).uuid(SERVICE_ID).appId(APP_ID).build();
    persistence.save(service);

    Map<String, String> configMapYamlByServiceTemplateId = new HashMap<>();
    configMapYamlByServiceTemplateId.put(SERVICE_TEMPLATE_ID, "abc");
    Environment environment = anEnvironment()
                                  .uuid(ENV_ID)
                                  .appId(APP_ID)
                                  .name(ENV_NAME)
                                  .configMapYamlByServiceTemplateId(configMapYamlByServiceTemplateId)
                                  .build();
    Application application =
        Application.Builder.anApplication().name(APP_NAME).uuid(APP_ID).accountId(ACCOUNT_ID).build();

    persistence.save(environment);
    persistence.save(application);

    ServiceTemplate serviceTemplate = aServiceTemplate()
                                          .withAppId(APP_ID)
                                          .withEnvId(ENV_ID)
                                          .withName(TEMPLATE_NAME)
                                          .withServiceId(SERVICE_ID)
                                          .withUuid(SERVICE_TEMPLATE_ID)
                                          .build();
    persistence.save(serviceTemplate);
    environmentService.deleteConfigMapYamlByServiceTemplateId(APP_ID, SERVICE_TEMPLATE_ID);
    Environment updatedEnvironment = persistence.get(Environment.class, ENV_ID);
    assertThat(updatedEnvironment.getConfigMapYamlByServiceTemplateId()).isNullOrEmpty();
    verify(yamlPushService, times(1))
        .pushYamlChangeSet(ACCOUNT_ID, environment, updatedEnvironment, Event.Type.UPDATE, false, false);
  }
}
