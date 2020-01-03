package software.wings.service.impl.artifactstream;

import static io.harness.rule.OwnerRule.GARVIT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceBuilder;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.ArtifactType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ArtifactStreamServiceBindingServiceTest extends WingsBaseTest {
  private static final String ANOTHER_SERVICE_ID = "ANOTHER_SERVICE_ID";
  private static final String ARTIFACT_STREAM_ID_1 = "ARTIFACT_STREAM_ID_1";
  private static final String ARTIFACT_STREAM_ID_2 = "ARTIFACT_STREAM_ID_2";
  private static final List<String> artifactStreamIds = Arrays.asList(ARTIFACT_STREAM_ID_1, ARTIFACT_STREAM_ID_2);
  @Mock private AppService appService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ArtifactStreamService artifactStreamService;
  @InjectMocks @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;

  @Before
  public void setUp() {
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldCreate() {
    ArtifactStream artifactStream1 = getArtifactStream(ARTIFACT_STREAM_ID_1, APP_ID);
    ArtifactStream artifactStream2 = getArtifactStream(ARTIFACT_STREAM_ID_2, APP_ID);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID_1)).thenReturn(artifactStream1);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID_2)).thenReturn(artifactStream2);

    Service service = getService().build();
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false)).thenReturn(service);

    ArtifactStream gotArtifactStream1 =
        artifactStreamServiceBindingService.createOld(APP_ID, SERVICE_ID, ARTIFACT_STREAM_ID_1);
    assertThat(gotArtifactStream1).isNotNull();
    assertThat(gotArtifactStream1.getUuid()).isEqualTo(ARTIFACT_STREAM_ID_1);

    ArtifactStream gotArtifactStream2 =
        artifactStreamServiceBindingService.createOld(APP_ID, SERVICE_ID, ARTIFACT_STREAM_ID_2);
    assertThat(gotArtifactStream2).isNotNull();
    assertThat(gotArtifactStream2.getUuid()).isEqualTo(ARTIFACT_STREAM_ID_2);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldDelete() {
    ArtifactStream artifactStream1 = getArtifactStream(ARTIFACT_STREAM_ID_1, APP_ID);
    ArtifactStream artifactStream2 = getArtifactStream(ARTIFACT_STREAM_ID_2, APP_ID);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID_1)).thenReturn(artifactStream1);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID_2)).thenReturn(artifactStream2);

    Service service = getService().artifactStreamIds(new ArrayList<>(artifactStreamIds)).build();
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false)).thenReturn(service);

    artifactStreamServiceBindingService.deleteOld(APP_ID, SERVICE_ID, ARTIFACT_STREAM_ID_1);
    assertThat(service.getArtifactStreamIds()).isEqualTo(Arrays.asList(ARTIFACT_STREAM_ID_2));

    artifactStreamServiceBindingService.deleteOld(APP_ID, SERVICE_ID, ARTIFACT_STREAM_ID_2);
    assertThat(service.getArtifactStreamIds()).isNullOrEmpty();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldListArtifactStreams() {
    ArtifactStream artifactStream1 = getArtifactStream(ARTIFACT_STREAM_ID_1, APP_ID);
    ArtifactStream artifactStream2 = getArtifactStream(ARTIFACT_STREAM_ID_2, APP_ID);
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(getService().artifactStreamIds(artifactStreamIds).build());
    when(artifactStreamService.listByIds(artifactStreamIds))
        .thenReturn(Arrays.asList(artifactStream1, artifactStream2));

    List<ArtifactStream> artifactStreams = artifactStreamServiceBindingService.listArtifactStreams(APP_ID, SERVICE_ID);
    assertThat(artifactStreams).isNotNull();
    assertThat(artifactStreams.stream().map(ArtifactStream::getUuid).collect(Collectors.toList()))
        .isEqualTo(artifactStreamIds);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldPruneByArtifactStream() {
    ArtifactStream artifactStream1 = getArtifactStream(ARTIFACT_STREAM_ID_1, APP_ID);
    ArtifactStream artifactStream2 = getArtifactStream(ARTIFACT_STREAM_ID_2, APP_ID);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID_1)).thenReturn(artifactStream1);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID_2)).thenReturn(artifactStream2);

    Service service1 = getService().artifactStreamIds(new ArrayList<>(artifactStreamIds)).build();

    Service service2 = getService()
                           .uuid(ANOTHER_SERVICE_ID)
                           .artifactStreamIds(new ArrayList<>(Arrays.asList(ARTIFACT_STREAM_ID_1)))
                           .build();

    when(serviceResourceService.listByArtifactStreamId(ARTIFACT_STREAM_ID_1)).thenReturn(asList(service1, service2));
    when(serviceResourceService.listByArtifactStreamId(ARTIFACT_STREAM_ID_2)).thenReturn(asList(service1));

    artifactStreamServiceBindingService.pruneByArtifactStream(APP_ID, ARTIFACT_STREAM_ID_1);
    assertThat(service1.getArtifactStreamIds()).isEqualTo(Arrays.asList(ARTIFACT_STREAM_ID_2));
    assertThat(service2.getArtifactStreamIds()).isNullOrEmpty();

    artifactStreamServiceBindingService.pruneByArtifactStream(APP_ID, ARTIFACT_STREAM_ID_2);
    assertThat(service1.getArtifactStreamIds()).isNullOrEmpty();
    assertThat(service2.getArtifactStreamIds()).isNullOrEmpty();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldPruneByArtifactStreamAtConnectorLevel() {
    ArtifactStream artifactStream1 = getArtifactStream(ARTIFACT_STREAM_ID_1, GLOBAL_APP_ID);
    ArtifactStream artifactStream2 = getArtifactStream(ARTIFACT_STREAM_ID_2, GLOBAL_APP_ID);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID_1)).thenReturn(artifactStream1);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID_2)).thenReturn(artifactStream2);

    Service service1 = getService().artifactStreamIds(new ArrayList<>(artifactStreamIds)).build();
    Service service2 = getService()
                           .uuid(ANOTHER_SERVICE_ID)
                           .artifactStreamIds(new ArrayList<>(Arrays.asList(ARTIFACT_STREAM_ID_1)))
                           .build();

    when(serviceResourceService.listByArtifactStreamId(ARTIFACT_STREAM_ID_1)).thenReturn(asList(service1, service2));
    when(serviceResourceService.listByArtifactStreamId(ARTIFACT_STREAM_ID_2)).thenReturn(asList(service1));

    artifactStreamServiceBindingService.pruneByArtifactStream(GLOBAL_APP_ID, ARTIFACT_STREAM_ID_1);
    assertThat(service1.getArtifactStreamIds()).isEqualTo(Arrays.asList(ARTIFACT_STREAM_ID_2));
    assertThat(service2.getArtifactStreamIds()).isNullOrEmpty();

    artifactStreamServiceBindingService.pruneByArtifactStream(GLOBAL_APP_ID, ARTIFACT_STREAM_ID_2);
    assertThat(service1.getArtifactStreamIds()).isNullOrEmpty();
    assertThat(service2.getArtifactStreamIds()).isNullOrEmpty();
  }

  private ServiceBuilder getService() {
    return Service.builder().appId(APP_ID).artifactType(ArtifactType.DOCKER).uuid(SERVICE_ID);
  }

  private ArtifactStream getArtifactStream(String id, String appId) {
    return JenkinsArtifactStream.builder()
        .uuid(id)
        .sourceName("todolistwar")
        .settingId(SETTING_ID)
        .appId(appId)
        .jobname("todolistwar")
        .autoPopulate(true)
        .serviceId(SERVICE_ID)
        .artifactPaths(asList("target/todolist.war"))
        .build();
  }
}
