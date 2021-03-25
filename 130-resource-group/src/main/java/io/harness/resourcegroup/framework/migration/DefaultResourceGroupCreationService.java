package io.harness.resourcegroup.framework.migration;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.resourcegroup.framework.service.ResourceGroupService;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class DefaultResourceGroupCreationService implements Managed {
  private final OrganizationService organizationService;
  private final ProjectService projectService;
  private final ResourceGroupService resourceGroupService;
  final ExecutorService executorService = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("default-resourcegroup-creation-worker-thread").build());
  Future defaultResourceGroupCreationFuture;

  @Override
  public void start() throws Exception {
    defaultResourceGroupCreationFuture = executorService.submit(this::defaultResourceGroupCreationJob);
  }

  @Override
  public void stop() throws Exception {
    defaultResourceGroupCreationFuture.cancel(true);
    executorService.shutdown();
    executorService.awaitTermination(5, TimeUnit.SECONDS);
  }

  private void defaultResourceGroupCreationJob() {
    Set<String> accountIds = new HashSet<>();
    int pageCounter = 0;
    int pageSize = 50;
    Page<Organization> organizationPage;
    while (true && !Thread.currentThread().isInterrupted()) {
      Pageable pageable = PageRequest.of(pageCounter, pageSize);
      organizationPage = organizationService.list(new Criteria(), pageable);
      if (!organizationPage.hasContent()) {
        break;
      }
      for (Organization organization : organizationPage.getContent()) {
        //      create account level default resourcegroup if not already created
        String accountIdentifier = organization.getAccountIdentifier();
        if (!accountIds.contains(accountIdentifier)) {
          resourceGroupService.ensureDefaultResourceGroup(accountIdentifier, null, null);
          accountIds.add(accountIdentifier);
        }
        //        Create orglevel default resourcegroup
        String orgIdentifier = organization.getIdentifier();
        resourceGroupService.ensureDefaultResourceGroup(accountIdentifier, orgIdentifier, null);
        //        Create project level default resoourcegroup
        Criteria criteria = Criteria.where(Project.ProjectKeys.accountIdentifier)
                                .is(accountIdentifier)
                                .and(Project.ProjectKeys.orgIdentifier)
                                .is(orgIdentifier);
        List<Project> projects = projectService.list(criteria);
        projects.forEach(project
            -> resourceGroupService.ensureDefaultResourceGroup(
                accountIdentifier, orgIdentifier, project.getIdentifier()));
      }
      pageCounter++;
    }
    if (Thread.currentThread().isInterrupted()) {
      log.error("Couldn't complete default resource group creation. Thread interrupted");
    } else {
      log.info("Default resource group creation for alreadly existing NG clients completed");
    }
  }
}
