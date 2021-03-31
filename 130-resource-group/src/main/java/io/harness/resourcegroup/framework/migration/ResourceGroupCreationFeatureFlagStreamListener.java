package io.harness.resourcegroup.framework.migration;

import static io.harness.beans.FeatureName.NG_ACCESS_CONTROL_MIGRATION;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.featureflag.FeatureFlagChangeDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.event.MessageListener;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.resourcegroup.framework.beans.ResourceGroupConstants;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.framework.service.ResourcePrimaryKey;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class ResourceGroupCreationFeatureFlagStreamListener implements MessageListener {
  private final OrganizationService orgService;
  private final ProjectService projectService;
  private final ResourceGroupService resourceGroupService;

  @SneakyThrows
  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      FeatureFlagChangeDTO featureFlagChangeDTO;
      try {
        featureFlagChangeDTO = FeatureFlagChangeDTO.parseFrom(message.getMessage().getData());
      } catch (InvalidProtocolBufferException e) {
        log.error("Unable to parse event into feature flag", e);
        return false;
      }
      if (NG_ACCESS_CONTROL_MIGRATION.equals(FeatureName.valueOf(featureFlagChangeDTO.getFeatureName()))
          && featureFlagChangeDTO.getEnable()) {
        try {
          return createDefaultResourceGroups(featureFlagChangeDTO.getAccountId());
        } catch (Exception ex) {
          log.error(
              "Error while processing {} feature flag for access control migration", NG_ACCESS_CONTROL_MIGRATION, ex);
          return false;
        }
      }
    }
    return true;
  }

  private boolean createDefaultResourceGroups(String accountId) {
    //    Creating account level default resource groups
    resourceGroupService.createDefaultResourceGroup(ResourcePrimaryKey.builder()
                                                        .accountIdentifier(accountId)
                                                        .resourceIdetifier(accountId)
                                                        .resourceType(ResourceGroupConstants.ACCOUNT)
                                                        .build());
    Criteria criteria = Criteria.where(OrganizationKeys.accountIdentifier).is(accountId);
    List<Organization> organizations = orgService.list(criteria);
    //    Creating org level default resource groups
    organizations.forEach(organization
        -> resourceGroupService.createDefaultResourceGroup(ResourcePrimaryKey.builder()
                                                               .accountIdentifier(organization.getAccountIdentifier())
                                                               .orgIdentifier(organization.getIdentifier())
                                                               .resourceType(ResourceGroupConstants.ORGANIZATION)
                                                               .resourceIdetifier(organization.getIdentifier())
                                                               .build()));

    criteria = Criteria.where(ProjectKeys.accountIdentifier).is(accountId);
    List<Project> projects = projectService.list(criteria);
    //    Creating project level default resource groups
    projects.forEach(project
        -> resourceGroupService.createDefaultResourceGroup(ResourcePrimaryKey.builder()
                                                               .accountIdentifier(project.getAccountIdentifier())
                                                               .orgIdentifier(project.getOrgIdentifier())
                                                               .projectIdentifer(project.getIdentifier())
                                                               .resourceType(ResourceGroupConstants.PROJECT)
                                                               .resourceIdetifier(project.getIdentifier())
                                                               .build()));
    return true;
  }
}
