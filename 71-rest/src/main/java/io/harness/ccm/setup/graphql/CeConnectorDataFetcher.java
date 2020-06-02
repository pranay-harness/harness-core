package io.harness.ccm.setup.graphql;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.ccm.health.HealthStatusServiceImpl;
import io.harness.ccm.setup.graphql.QLCEConnector.QLCEConnectorBuilder;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.beans.ce.CEGcpConfig;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CeConnectorDataFetcher
    extends AbstractConnectionV2DataFetcher<QLCESetupFilter, QLNoOpSortCriteria, QLCEConnectorData> {
  @Inject private CESetupQueryHelper ceSetupQueryHelper;
  @Inject private HealthStatusServiceImpl healthStatusService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLCEConnectorData fetchConnection(
      List<QLCESetupFilter> filters, QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<SettingAttribute> query = populateFilters(wingsPersistence, filters, SettingAttribute.class, true)
                                        .filter(SettingAttributeKeys.category, SettingCategory.CE_CONNECTOR);

    final List<SettingAttribute> settingAttributes = query.asList();
    List<QLCEConnector> connectorList = new ArrayList<>();
    for (SettingAttribute settingAttribute : settingAttributes) {
      connectorList.add(populateCEConnector(settingAttribute));
    }

    return QLCEConnectorData.builder().ceConnectors(connectorList).build();
  }

  private QLCEConnector populateCEConnector(SettingAttribute settingAttribute) {
    String accountName = settingAttribute.getName();
    QLCEConnectorBuilder qlCEConnectorBuilder =
        QLCEConnector.builder()
            .settingId(settingAttribute.getUuid())
            .accountName(accountName)
            .ceHealthStatus(healthStatusService.getHealthStatus(settingAttribute.getUuid()));
    if (settingAttribute.getValue() instanceof CEAwsConfig) {
      CEAwsConfig ceAwsConfig = (CEAwsConfig) settingAttribute.getValue();
      qlCEConnectorBuilder.curReportName(ceAwsConfig.getCurReportName())
          .s3BucketName(ceAwsConfig.getS3BucketDetails().getS3BucketName())
          .crossAccountRoleArn(ceAwsConfig.getAwsCrossAccountAttributes().getCrossAccountRoleArn())
          .infraType(QLInfraTypesEnum.AWS);
    } else if (settingAttribute.getValue() instanceof CEGcpConfig) {
      qlCEConnectorBuilder.infraType(QLInfraTypesEnum.GCP);
    }
    return qlCEConnectorBuilder.build();
  }

  @Override
  protected QLCESetupFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    return null;
  }

  @Override
  protected void populateFilters(List<QLCESetupFilter> filters, Query query) {
    ceSetupQueryHelper.setQuery(filters, query);
  }
}
