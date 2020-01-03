package software.wings.helpers.ext.bamboo;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.ANUBHAW;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ARTIFACT_PATH;

import com.google.common.util.concurrent.FakeTimeLimiter;
import com.google.inject.Inject;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.artifact.ArtifactFileMetadata;
import io.harness.delegate.exception.ArtifactServerException;
import io.harness.rule.Owner;
import io.harness.waiter.ListNotifyResponseData;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.BambooConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateFile;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.collect.artifacts.ArtifactCollectionTaskHelper;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.security.EncryptionService;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * Created by anubhaw on 12/8/16.
 */
public class BambooServiceTest extends WingsBaseTest {
  @Rule public WireMockRule wireMockRule = new WireMockRule(9095);
  @Inject @InjectMocks DelegateFileManager delegateFileManager;
  @Mock private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;

  @Mock private EncryptionService encryptionService;
  @InjectMocks private BambooService bambooService = new BambooServiceImpl();

  private BambooConfig bambooConfig = BambooConfig.builder()
                                          .bambooUrl("http://localhost:9095/")
                                          .username("admin")
                                          .password("admin".toCharArray())
                                          .build();
  @Before
  public void setupMocks() {
    on(bambooService).set("timeLimiter", new FakeTimeLimiter());
    on(bambooService).set("encryptionService", encryptionService);
    on(bambooService).set("artifactCollectionTaskHelper", artifactCollectionTaskHelper);
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldGetJobKeys() {}

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetPlanKeys() {
    assertThat(bambooService.getPlanKeys(bambooConfig, null))
        .contains(entry("TES-PLAN", "plan-1"), entry("TES-PLN10", "plan-10"), entry("TES-PLN100", "plan-100"),
            entry("TES-PLN1000", "plan-1000"), entry("TES-PLN10000", "plan-10000"), entry("TES-PLN1001", "plan-1001"));
  }

  @Test(expected = ArtifactServerException.class)
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetPlanKeysExceptionWithInvalidCreds() {
    wireMockRule.stubFor(get(urlEqualTo("/rest/api/latest/plan.json?authType=basic&max-results=1000"))
                             .willReturn(aResponse().withStatus(401).withHeader("Content-Type", "application/json")));
    bambooService.getPlanKeys(bambooConfig, null);
  }

  @Test(expected = ArtifactServerException.class)
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetPlanKeysException() {
    wireMockRule.stubFor(get(urlEqualTo("/rest/api/latest/plan.json?authType=basic&max-results=1000"))
                             .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json")));
    bambooService.getPlanKeys(bambooConfig, null);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetLastSuccessfulBuild() {
    BuildDetails buildDetails = bambooService.getLastSuccessfulBuild(bambooConfig, null, "TOD-TODIR");
    assertThat(buildDetails).isNotNull();
    assertThat(buildDetails.getNumber()).isEqualTo("104");
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetLastSuccessfulBuildWithArtifactFileMetadata() {
    BuildDetails buildDetails =
        bambooService.getLastSuccessfulBuild(bambooConfig, null, "TP-PLAN", asList("myartifacts/todolist.war"));
    assertThat(buildDetails).isNotNull();
    assertThat(buildDetails.getNumber()).isEqualTo("4");
    assertThat(buildDetails.getArtifactFileMetadataList()).isNotEmpty();
    assertThat(buildDetails.getArtifactFileMetadataList().get(0).getFileName()).isEqualTo("todolist.war");
    assertThat(buildDetails.getArtifactFileMetadataList().get(0).getUrl())
        .isEqualTo(
            "http://ec2-18-208-86-222.compute-1.amazonaws.com:8085/browse/TP-PLAN-4/artifact/JOB1/myartifacts/todolist.war");
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldNotGetLastSuccessfulBuild() {
    BuildDetails buildDetails = bambooService.getLastSuccessfulBuild(bambooConfig, null, "TOD-TOD");
    assertThat(buildDetails).isNull();
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetBuildsForJobWithEmptyArtifactPaths() {
    // backward compatibility - with artifact paths as ""
    List<BuildDetails> bamboo_plan_key = bambooService.getBuilds(bambooConfig, null, "TP-PLAN2", asList(""), 50);
    assertThat(bamboo_plan_key).isNotEmpty();
    assertThat(bamboo_plan_key.get(0)).extracting(BuildDetails::getNumber).isEqualTo("4");
    assertThat(bamboo_plan_key.get(0).getArtifactFileMetadataList()).isEmpty();
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetBuildsForJobWithCustomArtifactPaths() {
    List<BuildDetails> bamboo_plan_key = bambooService.getBuilds(
        bambooConfig, null, "TP-PLAN3", asList("myartifacts/todolist.war", "myartifacts/todolist.zip"), 50);
    assertThat(bamboo_plan_key).isNotEmpty();
    assertThat(bamboo_plan_key.get(0)).extracting(BuildDetails::getNumber).isEqualTo("4");
    assertThat(bamboo_plan_key.get(0).getArtifactFileMetadataList().get(0).getUrl())
        .isEqualTo("http://localhost:9095/browse/TP-PLAN3-4/artifact/JOB1/myartifacts/todolist.war");
    assertThat(bamboo_plan_key.get(0).getArtifactFileMetadataList().get(0).getFileName()).isEqualTo("todolist.war");
    assertThat(bamboo_plan_key.get(0).getArtifactFileMetadataList().get(1).getUrl())
        .isEqualTo("http://localhost:9095/browse/TP-PLAN3-4/artifact/JOB1/myartifacts/todolist.zip");
    assertThat(bamboo_plan_key.get(0).getArtifactFileMetadataList().get(1).getFileName()).isEqualTo("todolist.zip");
  }

  @Test(expected = ArtifactServerException.class)
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldGetBuildsForJobError() {
    wireMockRule.stubFor(get(
        urlEqualTo(
            "/rest/api/latest/result/BAMBOO_PLAN_KEY.json?authType=basic&buildState=Successful&expand=results.result&max-result=50"))
                             .willReturn(aResponse().withStatus(200).withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    bambooService.getBuilds(bambooConfig, null, "BAMBOO_PLAN_KEY", asList(ARTIFACT_PATH), 50);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetArtifactPath() {
    List<String> artifactPaths = bambooService.getArtifactPath(bambooConfig, null, "TP-PLAN");
    assertThat(artifactPaths).isNotEmpty();
    assertThat(artifactPaths.size()).isEqualTo(1);
    assertThat(artifactPaths).contains("myartifacts/todolist.war");
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldDownloadArtifact() throws FileNotFoundException {
    wireMockRule.stubFor(get(urlEqualTo("/artifact/TOD-TOD/JOB1/build-11/artifacts/todolist.tar"))
                             .willReturn(aResponse().withBody(new byte[] {1, 2, 3, 4})));
    ArtifactStreamAttributes artifactStreamAttributes =
        ArtifactStreamAttributes.builder()
            .jobName("TOD-TOD")
            .artifactPaths(asList("artifacts/todolist.tar"))
            .artifactFileMetadata(
                asList(ArtifactFileMetadata.builder()
                           .fileName("todolist.tar")
                           .url("http://localhost:9095/artifact/TOD-TOD/JOB1/build-11/artifacts/todolist.tar")
                           .build()))
            .build();
    ListNotifyResponseData listNotifyResponseData = new ListNotifyResponseData();
    DelegateFile delegateFile = DelegateFile.Builder.aDelegateFile().withFileId("FILE_ID").build();
    when(delegateFileManager.upload(any(), any())).thenReturn(delegateFile);
    bambooService.downloadArtifacts(
        bambooConfig, null, artifactStreamAttributes, "11", null, null, null, listNotifyResponseData);
    verify(artifactCollectionTaskHelper, times(1))
        .addDataToResponse(any(), eq("http://localhost:9095/artifact/TOD-TOD/JOB1/build-11/artifacts/todolist.tar"),
            any(), any(), any(), any());
  }
}
