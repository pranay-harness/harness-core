/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.rule.OwnerRule.ADWAIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.container.PcfServiceSpecification;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class ServiceHelperTest extends WingsBaseTest {
  @Inject @InjectMocks private ServiceHelper serviceHelper;

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testAddPlaceholderTexts() {
    PcfServiceSpecification pcfServiceSpecification =
        PcfServiceSpecification.builder()
            .serviceId("SERVICE_ID")
            .manifestYaml("  applications:\n"
                + "  - name : application\n"
                + "    memory: 850M\n"
                + "    instances : 2\n"
                + "    buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
                + "    path: /user/todo.war\n"
                + "    routes:\n"
                + "      - route: wings-apps-sf.cfapps.io\n"
                + "      - route: wings-apps-sf.cfapps.io\n")
            .build();

    serviceHelper.addPlaceholderTexts(pcfServiceSpecification);
    String manifest = pcfServiceSpecification.getManifestYaml();

    assertThat(manifest).isEqualTo("  applications:\n"
        + "  - name : ${APPLICATION_NAME}\n"
        + "    memory: 850M\n"
        + "    instances : ${INSTANCE_COUNT}\n"
        + "    buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
        + "    path: ${FILE_LOCATION}\n"
        + "    routes:\n"
        + "      - route: wings-apps-sf.cfapps.io\n"
        + "      - route: wings-apps-sf.cfapps.io\n");
  }
}
