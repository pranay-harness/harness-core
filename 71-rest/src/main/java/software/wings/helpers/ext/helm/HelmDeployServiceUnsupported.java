package software.wings.helpers.ext.helm;

import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.helpers.ext.helm.response.HelmListReleasesCommandResponse;
import software.wings.helpers.ext.helm.response.HelmReleaseHistoryCommandResponse;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class HelmDeployServiceUnsupported implements HelmDeployService {
  @Override
  public HelmCommandResponse deploy(HelmInstallCommandRequest commandRequest) {
    throw new UnsupportedOperationException("Helm deploy service not supported on manager");
  }

  @Override
  public HelmCommandResponse rollback(HelmRollbackCommandRequest commandRequest) {
    throw new UnsupportedOperationException("Helm deploy service not supported on manager");
  }

  @Override
  public HelmCommandResponse ensureHelmCliAndTillerInstalled(HelmCommandRequest helmCommandRequest) throws Exception {
    throw new UnsupportedOperationException("Helm deploy service not supported on manager");
  }

  @Override
  public HelmListReleasesCommandResponse listReleases(HelmInstallCommandRequest helmCommandRequest) {
    throw new UnsupportedOperationException("Helm deploy service not supported on manager");
  }

  @Override
  public HelmReleaseHistoryCommandResponse releaseHistory(HelmReleaseHistoryCommandRequest helmCommandRequest) {
    throw new UnsupportedOperationException("Helm deploy service not supported on manager");
  }

  @Override
  public HelmCommandResponse addPublicRepo(HelmCommandRequest commandRequest)
      throws InterruptedException, IOException, TimeoutException {
    throw new UnsupportedOperationException("Helm deploy service not supported on manager");
  }
}
