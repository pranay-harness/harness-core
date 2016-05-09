package software.wings.integration;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.ArtifactSource.ArtifactType.WAR;

import com.google.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.assertj.core.api.Assertions;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseIntegrationTest;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by anubhaw on 5/6/16.
 */
@Path("/dataGen")
public class DataGenIT extends WingsBaseIntegrationTest {
  private static final int NUM_APPS = 6;
  private static final int NUM_SERVICES_PER_APP = 6;
  private static final int NUM_ENV_PER_APP = 4;
  private static final int NUM_HOSTS_PER_INFRA = 100;

  private ClientConfig config = new ClientConfig(
      new JacksonJaxbJsonProvider().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));
  private Client client = ClientBuilder.newClient(config);

  @Inject private WingsPersistence wingsPersistence;

  @Before
  public void setUp() throws Exception {
    wingsPersistence.getDatastore().getDB().dropDatabase();
  }

  @Test
  public void populateTestData() throws IOException {
    List<Application> apps = createApplications();

    apps.forEach(application -> {
      try {
        addServices(application.getUuid());
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    });

    System.out.println(apps);
  }

  private void addServices(String appId) throws JsonProcessingException {
    List<String> serviceNames = Arrays.asList("Website", "Catalog", "Order", "Inventory", "Fulfillment", "Payment");

    WebTarget target = client.target("http://localhost:9090/wings/services/?app_id=" + appId);

    Map<String, String> serviceMap = new HashMap<>();
    serviceMap.put("name", serviceNames.get(0));
    serviceMap.put("description", serviceNames.get(0));
    serviceMap.put("appId", appId);
    serviceMap.put("artifactType", WAR.name());
    // TODO: Add containers

    //    String serviceJson = mapper.writeValueAsString(serviceMap);

    serviceNames.forEach(name -> {
      RestResponse<Service> response = target.request().post(
          Entity.entity(serviceMap, APPLICATION_JSON), new GenericType<RestResponse<Service>>() {});
      Assertions.assertThat(response.getResource()).isInstanceOf(Service.class);
    });
  }

  private List<Application> createApplications() {
    List<Application> apps = new ArrayList<>();

    List<String> appsName = getAppsName();
    WebTarget target = client.target("http://localhost:9090/wings/apps/");
    appsName.forEach(name -> {
      RestResponse<Application> response = target.request().post(
          Entity.entity(anApplication().withName(name).withDescription(name).build(), APPLICATION_JSON),
          new GenericType<RestResponse<Application>>() {});
      Assertions.assertThat(response.getResource()).isInstanceOf(Application.class);
      apps.add(response.getResource());
    });
    return apps;
  }

  private List<String> getAppsName() {
    List<String> names = new ArrayList<>();
    for (int i = 1; i <= NUM_APPS; i++) {
      names.add("App" + i);
    }
    return names;
  }

  private String randomString = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. "
      + "Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, "
      + "when an unknown printer took a galley of type and scrambled it to make a type specimen book. "
      + "It has survived not only five centuries, but also the leap into electronic typesetting, "
      + "remaining essentially unchanged. It was popularised in the 1960s with the release of "
      + "Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software "
      + "like Aldus PageMaker including versions of Lorem Ipsum";
}
