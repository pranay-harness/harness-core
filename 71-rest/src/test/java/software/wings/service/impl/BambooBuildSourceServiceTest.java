package software.wings.service.impl;

import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.BambooConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.service.intfc.BambooBuildService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.utils.ArtifactType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BambooBuildSourceServiceTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private SettingAttribute settingAttribute;
  private ArtifactStreamType streamType = ArtifactStreamType.BAMBOO;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Inject private BuildSourceService buildSourceService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private BambooBuildService bambooBuildService;
  @Inject private ScmSecret scmSecret;

  @Before
  public void setup() throws IllegalAccessException {
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    MockitoAnnotations.initMocks(this);
    when(delegateProxyFactory.get(Mockito.anyObject(), Mockito.any(SyncTaskContext.class)))
        .thenReturn(bambooBuildService);
    FieldUtils.writeField(buildSourceService, "delegateProxyFactory", delegateProxyFactory, true);
    settingAttribute =
        aSettingAttribute()
            .withName("bamboo")
            .withCategory(SettingCategory.CONNECTOR)
            .withAccountId(accountId)
            .withValue(BambooConfig.builder()
                           .accountId(accountId)
                           .bambooUrl("http://ec2-34-205-16-35.compute-1.amazonaws.com:8085/")
                           .username("wingsbuild")
                           .password(scmSecret.decryptToCharArray(new SecretName("bamboo_config_password")))
                           .build())
            .build();
    wingsPersistence.save(settingAttribute);
  }

  @Test
  @Owner(emails = RAGHU)
  @Category(UnitTests.class)
  @Ignore("Unit tests should not access external resources")
  public void getJobs() {
    Set<JobDetails> jobs = buildSourceService.getJobs(appId, settingAttribute.getUuid(), null);
    assertFalse(jobs.isEmpty());
  }

  @Test
  @Owner(emails = RAGHU)
  @Category(UnitTests.class)
  @Ignore("Unit tests should not access external resources")
  public void getPlans() {
    Map<String, String> plans = buildSourceService.getPlans(appId, settingAttribute.getUuid(), streamType.name());
    assertFalse(plans.isEmpty());
  }

  @Test
  @Owner(emails = RAGHU)
  @Category(UnitTests.class)
  @Ignore("Unit tests should not access external resources")
  public void getPlansWithType() {
    Service service = Service.builder().appId(appId).artifactType(ArtifactType.WAR).name("Some service").build();
    wingsPersistence.save(service);
    Map<String, String> plans =
        buildSourceService.getPlans(appId, settingAttribute.getUuid(), service.getUuid(), streamType.name(), "");
    assertFalse(plans.isEmpty());
  }

  @Test
  @Owner(emails = RAGHU)
  @Category(UnitTests.class)
  @Ignore("Unit tests should not access external resources")
  public void getArtifactPaths() {
    Set<String> artifactPaths =
        buildSourceService.getArtifactPaths(appId, "TOD-TOD", settingAttribute.getUuid(), null, streamType.name());
    assertFalse(artifactPaths.isEmpty());
    assertThat(artifactPaths.contains("artifacts/todolist.war")).isTrue();
  }

  @Test
  @Owner(emails = RAGHU)
  @Category(UnitTests.class)
  @Ignore("Unit tests should not access external resources")
  public void getBuilds() {
    Service service = Service.builder().appId(appId).artifactType(ArtifactType.WAR).name("Some service").build();
    wingsPersistence.save(service);
    BambooArtifactStream artifactStream = new BambooArtifactStream();
    artifactStream.setJobname("TOD-TOD");
    artifactStream.setArtifactPaths(Collections.singletonList("artifacts/todolist.war"));
    artifactStream.setServiceId(service.getUuid());
    artifactStream.setAppId(appId);
    wingsPersistence.save(artifactStream);

    List<BuildDetails> builds =
        buildSourceService.getBuilds(appId, artifactStream.getUuid(), settingAttribute.getUuid());
    assertFalse(builds.isEmpty());
  }

  @Test
  @Owner(emails = RAGHU)
  @Category(UnitTests.class)
  @Ignore("Unit tests should not access external resources")
  public void getLastSuccessfulBuild() {
    Service service = Service.builder().appId(appId).artifactType(ArtifactType.WAR).name("Some service").build();
    wingsPersistence.save(service);
    BambooArtifactStream artifactStream = new BambooArtifactStream();
    artifactStream.setJobname("TOD-TOD");
    artifactStream.setArtifactPaths(Collections.singletonList("artifacts/todolist.war"));
    artifactStream.setServiceId(service.getUuid());
    artifactStream.setAppId(appId);
    wingsPersistence.save(artifactStream);

    BuildDetails build =
        buildSourceService.getLastSuccessfulBuild(appId, artifactStream.getUuid(), settingAttribute.getUuid());
    assertNotNull(build);
  }
}
