package software.wings.helpers.ext.pcf;

import com.google.inject.Singleton;

import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import software.wings.beans.PcfConfig;

import java.util.List;

@Singleton
public class PcfDeploymentManagerUnsupported implements PcfDeploymentManager {
  public static final String DELIMITER = "__";

  @Override
  public List<String> getOrganizations(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported.");
  }

  @Override
  public List<String> getSpacesForOrganization(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public ApplicationDetail createApplication(PcfRequestConfig pcfRequestConfig, String manifestFilePath)
      throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public ApplicationDetail resizeApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public void deleteApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public String stopApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public ApplicationDetail getApplicationByName(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public void unmapRouteMapForApplication(PcfRequestConfig pcfRequestConfig, List<String> paths)
      throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public void mapRouteMapForApplication(PcfRequestConfig pcfRequestConfig, List<String> paths)
      throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public List<ApplicationSummary> getDeployedServicesWithNonZeroInstances(
      PcfRequestConfig pcfRequestConfig, String prefix) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public List<ApplicationSummary> getPreviousReleases(PcfRequestConfig pcfRequestConfig, String prefix)
      throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public List<String> getRouteMaps(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public String checkConnectivity(PcfConfig pcfConfig) {
    return "FAILED: connection timed out";
  }
}
