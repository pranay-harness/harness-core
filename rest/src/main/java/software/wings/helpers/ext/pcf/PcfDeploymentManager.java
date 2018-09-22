package software.wings.helpers.ext.pcf;

import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import software.wings.beans.PcfConfig;

import java.util.List;

public interface PcfDeploymentManager {
  List<String> getOrganizations(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  List<String> getSpacesForOrganization(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  ApplicationDetail createApplication(PcfRequestConfig pcfRequestConfig, String manifestFilePath)
      throws PivotalClientApiException;

  ApplicationDetail resizeApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  void deleteApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  String stopApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  ApplicationDetail getApplicationByName(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  void unmapRouteMapForApplication(PcfRequestConfig pcfRequestConfig, List<String> paths)
      throws PivotalClientApiException;

  void mapRouteMapForApplication(PcfRequestConfig pcfRequestConfig, List<String> paths)
      throws PivotalClientApiException;

  List<ApplicationSummary> getDeployedServicesWithNonZeroInstances(PcfRequestConfig pcfRequestConfig, String prefix)
      throws PivotalClientApiException;

  List<ApplicationSummary> getPreviousReleases(PcfRequestConfig pcfRequestConfig, String prefix)
      throws PivotalClientApiException;

  List<String> getRouteMaps(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  String checkConnectivity(PcfConfig pcfConfig);
}
