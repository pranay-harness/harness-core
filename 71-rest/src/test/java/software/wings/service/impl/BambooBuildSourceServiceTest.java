package software.wings.service.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.BambooConfig;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.generator.SecretGenerator;
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

/**
 * Created by rsingh on 10/9/17.
 */
@Ignore("Unit tests should not access external resources")
public class BambooBuildSourceServiceTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private SettingAttribute settingAttribute;
  private ArtifactStreamType streamType = ArtifactStreamType.BAMBOO;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Inject private BuildSourceService buildSourceService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private BambooBuildService bambooBuildService;
  @Inject private SecretGenerator secretGenerator;
  @Inject private ScmSecret scmSecret;

  @Before
  public void setup() {
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    MockitoAnnotations.initMocks(this);
    when(delegateProxyFactory.get(Mockito.anyObject(), Mockito.any(SyncTaskContext.class)))
        .thenReturn(bambooBuildService);
    setInternalState(buildSourceService, "delegateProxyFactory", delegateProxyFactory);
    settingAttribute =
        aSettingAttribute()
            .withName("bamboo")
            .withCategory(Category.CONNECTOR)
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
  public void getJobs() {
    Set<JobDetails> jobs = buildSourceService.getJobs(appId, settingAttribute.getUuid(), null);
    assertFalse(jobs.isEmpty());
  }

  @Test
  public void getPlans() {
    Map<String, String> plans = buildSourceService.getPlans(appId, settingAttribute.getUuid(), streamType.name());
    assertFalse(plans.isEmpty());
  }

  @Test
  public void getPlansWithType() {
    Service service = Service.builder().appId(appId).artifactType(ArtifactType.WAR).name("Some service").build();
    wingsPersistence.save(service);
    Map<String, String> plans =
        buildSourceService.getPlans(appId, settingAttribute.getUuid(), service.getUuid(), streamType.name(), "");
    assertFalse(plans.isEmpty());
  }

  @Test
  public void getArtifactPaths() {
    Set<String> artifactPaths =
        buildSourceService.getArtifactPaths(appId, "TOD-TOD", settingAttribute.getUuid(), null, streamType.name());
    assertFalse(artifactPaths.isEmpty());
    assertTrue(artifactPaths.contains("artifacts/todolist.war"));
  }

  @Test
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
