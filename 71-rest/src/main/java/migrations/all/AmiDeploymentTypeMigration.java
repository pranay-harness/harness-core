package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.beans.AmiDeploymentType;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureMappingService;

import java.util.List;

@Slf4j
public class AmiDeploymentTypeMigration implements Migration {
  @Inject private AppService appService;
  @Inject private AccountService accountService;
  @Inject private InfrastructureMappingService infraMappingService;

  @Override
  public void migrate() {
    PageRequest<Account> accountPageRequest = aPageRequest().addFieldsIncluded("_id").build();
    List<Account> accounts = accountService.list(accountPageRequest);
    if (isEmpty(accounts)) {
      return;
    }
    accounts.forEach(account -> {
      try {
        List<String> appIds = appService.getAppIdsByAccountId(account.getUuid());
        if (isEmpty(appIds)) {
          return;
        }
        appIds.forEach(appId -> {
          try {
            logger.info("Starting to perform Ami Deployment Type migration for app: [{}]", appId);
            PageRequest<InfrastructureMapping> pageRequest = new PageRequest<>();
            pageRequest.addFilter("appId", Operator.EQ, appId);
            PageResponse<InfrastructureMapping> response = infraMappingService.list(pageRequest);
            List<InfrastructureMapping> infraMappingList = response.getResponse();
            if (isEmpty(infraMappingList)) {
              logger.info("Done with Ami Deployment Type migration for app: [{}]", appId);
              return;
            }
            List<InfrastructureMapping> aminfraMappingList =
                infraMappingList.stream()
                    .filter(mapping -> mapping instanceof AwsAmiInfrastructureMapping)
                    .collect(toList());
            if (isEmpty(aminfraMappingList)) {
              logger.info("Done with Ami Deployment Type migration for app: [{}]", appId);
              return;
            }
            aminfraMappingList.forEach(amiMapping -> {
              AwsAmiInfrastructureMapping awsAmiInfrastructureMapping = (AwsAmiInfrastructureMapping) amiMapping;
              awsAmiInfrastructureMapping.setAmiDeploymentType(AmiDeploymentType.AWS_ASG);
              try {
                infraMappingService.update(awsAmiInfrastructureMapping);
              } catch (Exception ex) {
                logger.warn("Error while updating Ami Deployment Types for Aws Ami Infra Mapping: [{}]",
                    awsAmiInfrastructureMapping.getUuid(), ex);
              }
            });
            logger.info("Done with Ami Deployment Type migration for app: [{}]", appId);
          } catch (Exception ex) {
            logger.warn("Error while updating Ami Deployment Types for app: [{}]", appId, ex);
          }
        });
      } catch (Exception ex) {
        logger.warn("Error while updating Ami Deployment Types for account: [{}]", account.getUuid(), ex);
      }
    });
  }
}