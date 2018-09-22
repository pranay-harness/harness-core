package software.wings.service;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.helpers.ext.helm.HelmClient;
import software.wings.helpers.ext.helm.HelmClientImpl;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Created by anubhaw on 3/22/18.
 */
@Ignore
public class HelmClientTest {
  private static final Logger logger = LoggerFactory.getLogger(HelmClientTest.class);

  private HelmClient helmClient = new HelmClientImpl();
  @Test
  public void shouldInstall() throws InterruptedException, IOException, TimeoutException, ExecutionException {
    //    HelmCommandResponse helmCommandResponse =
    //        helmClient.install(HelmInstallCommandRequest.builder()
    //                               .chartName("/Users/anubhaw/work/helm-charts/todolist")
    //                               .namespace("default")
    //                               .releaseName("rel1")
    //                               .releaseVersion("1")
    //                               .timeoutInMillis(60000)
    //                               .valueOverrides(ImmutableMap.of("image.tag", "1.12.0-alpine"))
    //                               .build());
    //    System.out.println();
    //    System.out.println();
    //    System.out.println(helmCommandResponse.getOutput());
  }

  @Test
  public void shouldUpdate() throws InterruptedException, IOException, TimeoutException, ExecutionException {
    //    HelmCommandResponse helmCommandResponse =
    //        helmClient.upgrade(HelmInstallCommandRequest.builder()
    //                               .chartName("/Users/anubhaw/work/helm-charts/todolist")
    //                               .namespace("default")
    //                               .releaseName("AppName-Env-Service")
    //                               .releaseVersion("1")
    //                               .timeoutInMillis(60000)
    //                               .valueOverrides(ImmutableMap.of("image.tag", "1.13.7-alpine"))
    //                               .build());
    //    System.out.println();
    //    System.out.println();
    //    System.out.println(helmCommandResponse.getOutput());
  }

  @Test
  public void shouldRollback() throws InterruptedException, IOException, TimeoutException {
    HelmCommandResponse helmCommandResponse =
        helmClient.rollback(HelmRollbackCommandRequest.builder().releaseName("rel1").prevReleaseVersion(1).build());

    logger.info(helmCommandResponse.getOutput());
  }
}
