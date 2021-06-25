package io.harness.ng.resourcegroup.migration;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.remote.client.NGRestUtils;
import io.harness.resourcegroup.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;
import io.harness.utils.ScopeUtils;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class DefaultResourceGroupCreationService {
  private static final String DEFAULT_RESOURCE_GROUP_NAME = "All Resources";
  private static final String DEFAULT_RESOURCE_GROUP_IDENTIFIER = "_all_resources";
  private static final String DESCRIPTION_FORMAT = "All the resources in this %s are included in this resource group.";
  private final OrganizationService organizationService;
  private final ProjectService projectService;
  private final ResourceGroupClient resourceGroupClient;

  public void defaultResourceGroupCreationJob() {
    Set<String> accountIds = new HashSet<>();
    int pageCounter = 0;
    int pageSize = 50;
    Page<Organization> organizationPage;
    Criteria orgCriteria = Criteria.where(OrganizationKeys.deleted).is(Boolean.FALSE);
    SecurityContextBuilder.setContext(new ServicePrincipal(NG_MANAGER.getServiceId()));
    try {
      while (!Thread.currentThread().isInterrupted()) {
        Pageable pageable = PageRequest.of(pageCounter, pageSize);
        organizationPage = organizationService.list(orgCriteria, pageable);
        if (!organizationPage.hasContent()) {
          break;
        }
        for (Organization organization : organizationPage.getContent()) {
          //      create account level default resourcegroup if not already created
          String accountIdentifier = organization.getAccountIdentifier();
          if (!accountIds.contains(accountIdentifier)) {
            createDefaultResourceGroup(accountIdentifier, null, null);
            accountIds.add(accountIdentifier);
          }

          //        Create orglevel default resourcegroup
          String orgIdentifier = organization.getIdentifier();
          createDefaultResourceGroup(accountIdentifier, orgIdentifier, null);

          //        Create project level default resourcegroup
          Criteria projectCriteria = Criteria.where(ProjectKeys.accountIdentifier)
                                         .is(accountIdentifier)
                                         .and(ProjectKeys.orgIdentifier)
                                         .is(orgIdentifier)
                                         .and(ProjectKeys.deleted)
                                         .is(Boolean.FALSE);
          List<Project> projects = projectService.list(projectCriteria);
          projects.forEach(project -> {
            String projectIdentifier = project.getIdentifier();
            createDefaultResourceGroup(accountIdentifier, orgIdentifier, projectIdentifier);
          });
        }
        pageCounter++;
      }
    } catch (Exception exception) {
      log.error("Couldn't complete default resource group creation. Exception occurred", exception);
    }
    SecurityContextBuilder.unsetCompleteContext();
    if (Thread.currentThread().isInterrupted()) {
      log.error("Couldn't complete default resource group creation. Thread interrupted");
    } else {
      log.info("Default resource group creation for already existing NG clients completed");
    }
  }

  public void createDefaultResourceGroup(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    try {
      ResourceGroupResponse resourceGroupResponse = NGRestUtils.getResponse(resourceGroupClient.getResourceGroup(
          DEFAULT_RESOURCE_GROUP_IDENTIFIER, accountIdentifier, orgIdentifier, projectIdentifier));
      if (resourceGroupResponse != null) {
        return;
      }
      ResourceGroupDTO resourceGroupDTO = getResourceGroupDTO(accountIdentifier, orgIdentifier, projectIdentifier);
      NGRestUtils.getResponse(resourceGroupClient.createManagedResourceGroup(
          accountIdentifier, orgIdentifier, projectIdentifier, resourceGroupDTO));
    } catch (Exception e) {
      log.error("Couldn't create default resource group for {}",
          ScopeUtils.toString(accountIdentifier, orgIdentifier, projectIdentifier));
    }
  }

  private ResourceGroupDTO getResourceGroupDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return ResourceGroupDTO.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .name(DEFAULT_RESOURCE_GROUP_NAME)
        .identifier(DEFAULT_RESOURCE_GROUP_IDENTIFIER)
        .description(String.format(DESCRIPTION_FORMAT,
            ScopeUtils.getMostSignificantScope(accountIdentifier, orgIdentifier, projectIdentifier)
                .toString()
                .toLowerCase()))
        .resourceSelectors(Collections.emptyList())
        .fullScopeSelected(true)
        .build();
  }
}
