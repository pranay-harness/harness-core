package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Host.Builder.aHost;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.Setup.SetupStatus.COMPLETE;
import static software.wings.beans.Setup.SetupStatus.INCOMPLETE;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Application;
import software.wings.beans.Application.Builder;
import software.wings.beans.Artifact;
import software.wings.beans.JenkinsArtifactSource;
import software.wings.beans.Release;
import software.wings.beans.Setup;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ReleaseService;
import software.wings.service.intfc.SetupService;
import software.wings.service.intfc.WorkflowService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 6/30/16.
 */
public class SetupServiceImplTest extends WingsBaseTest {
  @Mock private HostService hostService;
  @Mock private ReleaseService releaseService;
  @Mock private ArtifactService artifactService;
  @Mock private WorkflowService workflowService;
  @Mock private MainConfiguration configuration;

  @Inject @InjectMocks private SetupService setupService;

  @Mock private PortalConfig portalConfig;

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(configuration.getPortal()).thenReturn(portalConfig);
    when(portalConfig.getUrl()).thenReturn("http://localhost:9090/wings/");
  }

  /**
   * Should get application setup status.
   */
  @Test
  public void shouldGetApplicationSetupStatus() {
    Application application = Builder.anApplication()
                                  .withUuid(APP_ID)
                                  .withServices(asList(aService().withUuid(SERVICE_ID).build()))
                                  .withEnvironments(asList(anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).build()))
                                  .build();
    when(hostService.getHostsByEnv(APP_ID, ENV_ID)).thenReturn(asList(aHost().build()));
    Setup setupStatus = setupService.getApplicationSetupStatus(application);
    assertThat(setupStatus.getSetupStatus()).isEqualTo(COMPLETE);
    assertThat(setupStatus.getActions()).hasSize(1).doesNotContainNull();
    assertThat(setupStatus.getActions().get(0).getCode()).isEqualTo("NO_RELEASE_FOUND");

    Release rel = Release.Builder.aRelease().build();
    PageResponse<Release> res = new PageResponse();
    res.setResponse(asList(rel));
    when(releaseService.list(any(PageRequest.class))).thenReturn(res);
    setupStatus = setupService.getApplicationSetupStatus(application);
    assertThat(setupStatus.getSetupStatus()).isEqualTo(COMPLETE);
    assertThat(setupStatus.getActions()).hasSize(1).doesNotContainNull();
    assertThat(setupStatus.getActions().get(0).getCode()).isEqualTo("NO_ARTIFACT_SOURCE_FOUND");

    rel.setArtifactSources(asList(new JenkinsArtifactSource()));
    setupStatus = setupService.getApplicationSetupStatus(application);
    assertThat(setupStatus.getSetupStatus()).isEqualTo(COMPLETE);
    assertThat(setupStatus.getActions()).hasSize(1).doesNotContainNull();
    assertThat(setupStatus.getActions().get(0).getCode()).isEqualTo("NO_ARTIFACT_FOUND");

    PageResponse<Artifact> artRes = new PageResponse<>();
    artRes.setResponse(asList(new Artifact()));
    when(artifactService.list(any(PageRequest.class))).thenReturn(artRes);
    setupStatus = setupService.getApplicationSetupStatus(application);
    assertThat(setupStatus.getSetupStatus()).isEqualTo(COMPLETE);
    assertThat(setupStatus.getActions()).hasSize(1).doesNotContainNull();
    assertThat(setupStatus.getActions().get(0).getCode()).isEqualTo("NO_DEPLOYMENT_FOUND");

    PageResponse<WorkflowExecution> execRes = new PageResponse<>();
    execRes.setResponse(asList(new WorkflowExecution()));
    when(workflowService.listExecutions(any(PageRequest.class), anyBoolean())).thenReturn(execRes);
    setupStatus = setupService.getApplicationSetupStatus(application);
    assertThat(setupStatus.getSetupStatus()).isEqualTo(COMPLETE);
    assertThat(setupStatus.getActions()).isEmpty();
  }

  /**
   * Should get incomplete status for application without service.
   */
  @Test
  public void shouldGetIncompleteStatusForApplicationWithoutService() {
    Application application = Builder.anApplication().withUuid(APP_ID).build();
    Setup setupStatus = setupService.getApplicationSetupStatus(application);
    assertThat(setupStatus.getSetupStatus()).isEqualTo(INCOMPLETE);
    assertThat(setupStatus.getActions().size()).isEqualTo(1);
    assertThat(setupStatus.getActions().get(0).getCode()).isEqualTo("SERVICE_NOT_CONFIGURED");
  }

  /**
   * Should get incomplete status for application without environment.
   */
  @Test
  public void shouldGetIncompleteStatusForApplicationWithoutEnvironment() {
    Application application =
        Builder.anApplication().withUuid(APP_ID).withServices(asList(aService().withUuid(SERVICE_ID).build())).build();
    Setup setupStatus = setupService.getApplicationSetupStatus(application);
    assertThat(setupStatus.getSetupStatus()).isEqualTo(INCOMPLETE);
    assertThat(setupStatus.getActions().size()).isEqualTo(1);
    assertThat(setupStatus.getActions().get(0).getCode()).isEqualTo("ENVIRONMENT_NOT_CONFIGURED");
  }

  /**
   * Should get service setup status.
   */
  @Test
  public void shouldGetServiceSetupStatus() {
    Setup setupStatus = setupService.getServiceSetupStatus(aService().build());
    assertThat(setupStatus.getSetupStatus()).isEqualTo(COMPLETE);
    assertThat(setupStatus.getActions()).isEmpty();
  }

  /**
   * Should get environment setup status.
   */
  @Test
  public void shouldGetEnvironmentSetupStatus() {
    when(hostService.getHostsByEnv(APP_ID, ENV_ID)).thenReturn(asList(aHost().build()));
    Setup setupStatus =
        setupService.getEnvironmentSetupStatus(anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).build());
    assertThat(setupStatus.getSetupStatus()).isEqualTo(COMPLETE);
    assertThat(setupStatus.getActions()).isEmpty();
  }

  /**
   * Should get incomplete status for environment with no host.
   */
  @Test
  public void shouldGetIncompleteStatusForEnvironmentWithNoHost() {
    when(hostService.getHostsByEnv(APP_ID, ENV_ID)).thenReturn(asList());
    Setup setupStatus =
        setupService.getEnvironmentSetupStatus(anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).build());
    assertThat(setupStatus.getSetupStatus()).isEqualTo(INCOMPLETE);
    assertThat(setupStatus.getActions().size()).isEqualTo(1);
    assertThat(setupStatus.getActions().get(0).getCode()).isEqualTo("NO_HOST_CONFIGURED");
  }
}
