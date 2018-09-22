package software.wings.integration.service;

import static java.util.Arrays.asList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.ArtifactType.WAR;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.intfc.ServiceResourceService;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by sgurubelli on 7/28/17.
 */
@Ignore
public class ArtifactStreamServiceIntegrationTest extends BaseIntegrationTest {
  @Inject ServiceResourceService serviceResourceService;

  private static final String APP_NAME = "APP_NAME_ARTIFACT";
  private static final String SERVICE_NAME = "SERVICE_NAME_SERVICE";

  private JenkinsArtifactStream jenkinsArtifactStream = JenkinsArtifactStream.builder()
                                                            .appId(APP_ID)
                                                            .uuid(ARTIFACT_STREAM_ID)
                                                            .sourceName("SOURCE_NAME")
                                                            .settingId(SETTING_ID)
                                                            .jobname("JOB")
                                                            .serviceId(SERVICE_ID)
                                                            .artifactPaths(asList("*WAR"))
                                                            .build();

  @Test
  public void testAddArtifactStream() {
    loginAdminUser();
    Application app = createApp(APP_NAME);
    Service service = createService(app.getAppId(),
        ImmutableMap.of(
            "name", SERVICE_NAME, "description", randomText(40), "appId", app.getUuid(), "artifactType", WAR.name()));
    addArtifactStream(app.getAppId(), service.getUuid());
  }

  private void addArtifactStream(String appId, String serviceId) {
    WebTarget target = client.target(API_BASE + "/artifactstreams/?appId=" + appId);
    SettingAttribute settingAttribute =
        wingsPersistence.createQuery(SettingAttribute.class).filter("name", "Harness Jenkins").get();

    jenkinsArtifactStream = JenkinsArtifactStream.builder()
                                .appId(APP_ID)
                                .uuid(ARTIFACT_STREAM_ID)
                                .sourceName("todolistwar")
                                .settingId(settingAttribute.getUuid())
                                .jobname("toddolistwar")
                                .serviceId(serviceId)
                                .artifactPaths(asList("target/todolistwar"))
                                .build();
    RestResponse<ArtifactStream> response = getRequestBuilder(target).post(
        entity(jenkinsArtifactStream, APPLICATION_JSON), new GenericType<RestResponse<ArtifactStream>>() {

        });
    assertThat(response).isNotNull();
  }
}
