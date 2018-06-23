package software.wings.helpers.ext.pcf;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static software.wings.helpers.ext.pcf.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import software.wings.utils.Misc;

import java.util.Collections;
import java.util.List;

@Singleton
public class PcfDeploymentManagerImpl implements PcfDeploymentManager {
  public static final String DELIMITER = "__";
  @Inject PcfClient pcfClient;

  public List<String> getOrganizations(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      List<OrganizationSummary> organizationSummaries = pcfClient.getOrganizations(pcfRequestConfig);
      return organizationSummaries.stream().map(organizationSummary -> organizationSummary.getName()).collect(toList());
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + Misc.getMessage(e), e);
    }
  }

  public List<String> getSpacesForOrganization(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      return pcfClient.getSpacesForOrganization(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + Misc.getMessage(e), e);
    }
  }

  public List<String> getRouteMaps(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      return pcfClient.getRoutesForSpace(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + Misc.getMessage(e), e);
    }
  }

  public ApplicationDetail createApplication(PcfRequestConfig pcfRequestConfig, String manifestFilePath)
      throws PivotalClientApiException {
    try {
      pcfClient.pushApplicationUsingManifest(pcfRequestConfig, manifestFilePath);
      return getApplicationByName(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + Misc.getMessage(e), e);
    }
  }

  public ApplicationDetail getApplicationByName(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      return pcfClient.getApplicationByName(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + Misc.getMessage(e), e);
    }
  }

  public ApplicationDetail resizeApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      ApplicationDetail applicationDetail = pcfClient.getApplicationByName(pcfRequestConfig);
      pcfClient.scaleApplications(pcfRequestConfig);
      if (pcfRequestConfig.getDesiredCount() > 0 && applicationDetail.getInstances() == 0) {
        pcfClient.startApplication(pcfRequestConfig);
      }

      // is scales down to 0, stop application
      if (pcfRequestConfig.getDesiredCount() == 0) {
        pcfClient.stopApplication(pcfRequestConfig);
      }

      return pcfClient.getApplicationByName(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + Misc.getMessage(e), e);
    }
  }

  public void unmapRouteMapForApplication(PcfRequestConfig pcfRequestConfig, List<String> paths)
      throws PivotalClientApiException {
    try {
      pcfClient.unmapRoutesForApplication(pcfRequestConfig, paths);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + Misc.getMessage(e), e);
    }
  }

  public void mapRouteMapForApplication(PcfRequestConfig pcfRequestConfig, List<String> paths)
      throws PivotalClientApiException {
    try {
      pcfClient.mapRoutesForApplication(pcfRequestConfig, paths);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + Misc.getMessage(e), e);
    }
  }

  @Override
  public List<ApplicationSummary> getDeployedServicesWithNonZeroInstances(
      PcfRequestConfig pcfRequestConfig, String prefix) throws PivotalClientApiException {
    try {
      List<ApplicationSummary> applicationSummaries = pcfClient.getApplications(pcfRequestConfig);
      if (CollectionUtils.isEmpty(applicationSummaries)) {
        return Collections.EMPTY_LIST;
      }

      return applicationSummaries.stream()
          .filter(applicationSummary
              -> applicationSummary.getName().startsWith(prefix) && applicationSummary.getInstances() > 0)
          .sorted(comparingInt(applicationSummary -> getRevisionFromServiceName(applicationSummary.getName())))
          .collect(toList());

    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + Misc.getMessage(e), e);
    }
  }

  @Override
  public List<ApplicationSummary> getPreviousReleases(PcfRequestConfig pcfRequestConfig, String prefix)
      throws PivotalClientApiException {
    try {
      List<ApplicationSummary> applicationSummaries = pcfClient.getApplications(pcfRequestConfig);
      if (CollectionUtils.isEmpty(applicationSummaries)) {
        return Collections.EMPTY_LIST;
      }

      return applicationSummaries.stream()
          .filter(applicationSummary -> applicationSummary.getName().startsWith(prefix))
          .sorted(comparingInt(applicationSummary -> getRevisionFromServiceName(applicationSummary.getName())))
          .collect(toList());

    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + Misc.getMessage(e), e);
    }
  }

  public void deleteApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      pcfClient.deleteApplication(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + Misc.getMessage(e), e);
    }
  }

  public String stopApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      pcfClient.stopApplication(pcfRequestConfig);
      return getDetailedApplicationState(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + Misc.getMessage(e), e);
    }
  }

  private String getDetailedApplicationState(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    ApplicationDetail applicationDetail = pcfClient.getApplicationByName(pcfRequestConfig);
    return new StringBuilder("Application Created : ")
        .append(applicationDetail.getName())
        .append(", Details: ")
        .append(applicationDetail.toString())
        .toString();
  }

  public static int getRevisionFromServiceName(String name) {
    if (name != null) {
      int index = name.lastIndexOf(DELIMITER);
      if (index >= 0) {
        try {
          return Integer.parseInt(name.substring(index + DELIMITER.length()));
        } catch (NumberFormatException e) {
          // Ignore
        }
      }
    }
    return -1;
  }
}
