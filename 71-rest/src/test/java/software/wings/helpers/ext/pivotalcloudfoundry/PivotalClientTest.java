package software.wings.helpers.ext.pivotalcloudfoundry;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.helpers.ext.pcf.PcfClient;
import software.wings.helpers.ext.pcf.PcfClientImpl;

//

public class PivotalClientTest extends WingsBaseTest {
  PcfClient pcfClient = new PcfClientImpl();

  @Test
  public void testGet() throws Exception {
    /*  PcfRequestConfig pcfRequestConfig = PcfRequestConfig.builder()
                                              .userName("adwait.bhandare@harness.io")
                                              .password(secretGenerator.decryptToCharArray(new
    SecretName("pcf_request_config_password"))) .endpointUrl("api.run.pivotal.io") .build();

      // Get Organization list
      List<OrganizationSummary> organizationList = pcfClient.getOrganizations(pcfRequestConfig);
      assertNotNull(organizationList);
      assertEquals(1, organizationList.size());

      String organization = organizationList.get(0).getName();
      assertEquals("Ad-PCF", organization);

      // get Spaces for organization
      pcfRequestConfig.setOrgName(organization);
      List<String> spaces = pcfClient.getSpacesForOrganization(pcfRequestConfig);
      System.out.println(spaces);
      assertNotNull(spaces);
      assertEquals(1, spaces.size());
      assertEquals("development", spaces.get(0).toLowerCase());

      // push an application with 1 instance
      pcfRequestConfig.setSpaceName(spaces.get(0));
      pcfRequestConfig.setApplicationName("harness-pcf-app-deploy-version");
      pcfClient.pushApplicationUsingManifest(pcfRequestConfig, prepareManifestYamlFile());

      pcfRequestConfig.setApplicationName("harness-pcf-app-deploy-version");
      ApplicationDetail applicationDetail = pcfClient.getApplicationByName(pcfRequestConfig);
      assertNotNull(applicationDetail);
      assertEquals("harness-pcf-app-deploy-version", applicationDetail.getName());
      assertTrue(applicationDetail.getUrls().contains("wings-apps-sf.cfapps.io"));

      // scale app to 2 instances
      pcfRequestConfig.setDesiredCount(2);
      pcfClient.scaleApplications(pcfRequestConfig);
      for (int count = 0; count < 5; count++) {
        applicationDetail = pcfClient.getApplicationByName(pcfRequestConfig);
        if (applicationDetail.getInstances().intValue() == 2
            && "RUNNING".equals(applicationDetail.getInstanceDetails().get(0).getState())
            && "RUNNING".equals(applicationDetail.getInstanceDetails().get(1).getState())) {
          break;
        }

        Thread.sleep(20000);
      }
      assertEquals(2, applicationDetail.getInstances().intValue());
      assertEquals("RUNNING", applicationDetail.getInstanceDetails().get(0).getState());
      assertEquals("RUNNING", applicationDetail.getInstanceDetails().get(1).getState());

      // scale app to 1 instance
      pcfRequestConfig.setDesiredCount(1);
      pcfClient.scaleApplications(pcfRequestConfig);
      for (int count = 0; count < 5; count++) {
        applicationDetail = pcfClient.getApplicationByName(pcfRequestConfig);

        if (applicationDetail.getInstances().intValue() == 1
            && "RUNNING".equals(applicationDetail.getInstanceDetails().get(0).getState())) {
          break;
        }

        Thread.sleep(20000);
      }
      assertEquals(1, applicationDetail.getInstances().intValue());
      assertEquals("RUNNING", applicationDetail.getInstanceDetails().get(0).getState());

      // stop application
      pcfClient.stopApplication(pcfRequestConfig);
      applicationDetail = pcfClient.getApplicationByName(pcfRequestConfig);
      assertEquals(0, applicationDetail.getRunningInstances().intValue());
      assertTrue(applicationDetail.getUrls().contains("wings-apps-sf.cfapps.io"));

      // unmap routeMap for application
      Optional<Route> route = pcfClient.getRouteMap("wings-apps-sf.cfapps.io", pcfRequestConfig);
      pcfClient.unmapRouteMapForApp(pcfRequestConfig, route.get());
      // is scales down to 0, stop application
      if (pcfRequestConfig.getDesiredCount() == 0) {
        pcfClient.stopApplication(pcfRequestConfig);
      }

      applicationDetail = pcfClient.getApplicationByName(pcfRequestConfig);
      assertEquals(0, applicationDetail.getUrls().size());

      // delete application
      pcfClient.deleteApplication(pcfRequestConfig);
      try {
        applicationDetail = pcfClient.getApplicationByName(pcfRequestConfig);
        // Exception expected as application does not exist
        assertFalse(true);
      } catch (PivotalClientApiException e) {
      }
    }

    private String prepareManifestYamlFile() throws IOException {
      ClassLoader classLoader = getClass().getClassLoader();
      File artifact = new File(classLoader.getResource("todolist.war").getFile());
      File manifest = new File(classLoader.getResource("manifest.yml").getFile());
      String path = manifest.getAbsolutePath();
      manifest.delete();
      manifest = new File(path);

      String manifestYaml = "---\n"
          + "applications:\n"
          + "- name: harness-pcf-app-deploy-version\n"
          + "  memory: 700M\n"
          + "  instances: 0\n"
          + "  buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
          + "  path: " + artifact.getAbsolutePath() + "\n"
          + "  routes:\n"
          + "  - route: wings-apps-sf.cfapps.io";

      BufferedWriter writer = new BufferedWriter(new FileWriter(manifest));
      writer.write(manifestYaml);
      writer.close();
      return path;*/
  }
}
