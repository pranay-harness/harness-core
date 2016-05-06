package software.wings.integration;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.beans.Application.Builder.anApplication;

import com.google.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseIntegrationTest;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.dl.WingsPersistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
  private static final int NUM_APPS = 10;
  private static final int NUM_SERVICES_PER_APP = 6;
  private static final int NUM_ENV_PER_APP = 4;
  private static final int NUM_HOSTS_PER_INFRA = 100;

  private Client client = ClientBuilder.newClient();
  private ObjectMapper mapper = new ObjectMapper().disable(FAIL_ON_UNKNOWN_PROPERTIES);

  @Inject private WingsPersistence wingsPersistence;

  @Before
  public void setUp() throws Exception {
    wingsPersistence.getDatastore().getDB().dropDatabase();
  }

  @Test
  public void populateTestData() throws IOException {
    WebTarget target = client.target("http://localhost:9090/wings/apps/");

    List<String> appsName = getAppsName();
    appsName.forEach(name -> {
      RestResponse<Application> response = target.request().post(
          Entity.entity(anApplication().withName(name).withDescription(name).build(), APPLICATION_JSON),
          new GenericType<RestResponse<Application>>() {});
      Assertions.assertThat(response.getResource()).isInstanceOf(Application.class);
    });
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
