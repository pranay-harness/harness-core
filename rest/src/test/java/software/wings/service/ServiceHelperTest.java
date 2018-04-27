package software.wings.service;

import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.container.PcfServiceSpecification;

public class ServiceHelperTest extends WingsBaseTest {
  @Inject @InjectMocks private ServiceHelper serviceHelper;

  @Test
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
                + "      - route: wings-apps-sf.cfapps.io\n"
                + "serviceName: SERV")
            .build();

    serviceHelper.addPlaceholderTexts(pcfServiceSpecification);
    String manifest = pcfServiceSpecification.getManifestYaml();

    assertEquals("  applications:\n"
            + "  - name : ${APPLICATION_NAME}\n"
            + "    memory: 850M\n"
            + "    instances : ${INSTANCE_COUNT}\n"
            + "    buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
            + "    path: ${FILE_LOCATION}\n"
            + "    routes:\n"
            + "      - route: ${ROUTE_MAP}\n"
            + "serviceName: SERV\n",
        manifest);
  }
}
