package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationList;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLFilterValuesData;
import software.wings.graphql.schema.type.aggregation.billing.QLFilterValuesData.QLFilterValuesDataBuilder;
import software.wings.graphql.schema.type.aggregation.billing.QLFilterValuesListData;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

@Slf4j
public class BillingStatsFilterValuesDataFetcher
    extends AbstractStatsDataFetcherWithAggregationList<QLCCMAggregationFunction, QLBillingDataFilter, QLCCMGroupBy,
        QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject QLBillingStatsHelper statsHelper;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sortCriteria) {
    try {
      if (timeScaleDBService.isValid()) {
        return getEntityData(accountId, filters, groupBy);
      } else {
        throw new InvalidRequestException("Cannot process request in BillingStatsFilterValuesDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching billing data {}", e);
    }
  }

  protected QLFilterValuesListData getEntityData(
      @NotNull String accountId, List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupByList) {
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupByList);
    queryData = billingDataQueryBuilder.formFilterValuesQuery(accountId, filters, groupByEntityList);
    logger.info("BillingStatsFilterValuesDataFetcher query!! {}", queryData.getQuery());

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      return generateFilterValuesData(queryData, resultSet);
    } catch (SQLException e) {
      logger.error("BillingStatsFilterValuesDataFetcher Error exception", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return null;
  }

  private QLFilterValuesListData generateFilterValuesData(BillingDataQueryMetadata queryData, ResultSet resultSet)
      throws SQLException {
    QLFilterValuesDataBuilder filterValuesDataBuilder = QLFilterValuesData.builder();
    Set<String> cloudServiceNames = new HashSet<>();
    Set<String> workloadNames = new HashSet<>();
    Set<String> launchTypes = new HashSet<>();
    Set<String> instanceIds = new HashSet<>();
    Set<String> namespaces = new HashSet<>();
    Set<String> applicationIds = new HashSet<>();
    Set<String> environmentIds = new HashSet<>();
    Set<String> cloudProviders = new HashSet<>();
    Set<String> serviceIds = new HashSet<>();
    List<QLEntityData> clusters = new ArrayList<>();
    while (resultSet != null && resultSet.next()) {
      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field) {
          case CLOUDSERVICENAME:
            cloudServiceNames.add(resultSet.getString(field.getFieldName()));
            break;
          case LAUNCHTYPE:
            launchTypes.add(resultSet.getString(field.getFieldName()));
            break;
          case INSTANCEID:
            instanceIds.add(resultSet.getString(field.getFieldName()));
            break;
          case CLUSTERID:
            clusters.add(QLEntityData.builder()
                             .name(resultSet.getString(BillingDataMetaDataFields.CLUSTERNAME.getFieldName()))
                             .id(resultSet.getString(field.getFieldName()))
                             .type(resultSet.getString(BillingDataMetaDataFields.CLUSTERTYPE.getFieldName()))
                             .build());
            break;
          case NAMESPACE:
            namespaces.add(resultSet.getString(field.getFieldName()));
            break;
          case WORKLOADNAME:
            workloadNames.add(resultSet.getString(field.getFieldName()));
            break;
          case APPID:
            applicationIds.add(resultSet.getString(field.getFieldName()));
            break;
          case CLOUDPROVIDER:
            cloudProviders.add(resultSet.getString(field.getFieldName()));
            break;
          case ENVID:
            environmentIds.add(resultSet.getString(field.getFieldName()));
            break;
          case SERVICEID:
            serviceIds.add(resultSet.getString(field.getFieldName()));
            break;
          default:
            break;
        }
      }
    }

    filterValuesDataBuilder.cloudServiceNames(cloudServiceNames.toArray(new String[0]))
        .instanceIds(instanceIds.toArray(new String[0]))
        .launchTypes(launchTypes.toArray(new String[0]))
        .clusters(clusters)
        .namespaces(namespaces.toArray(new String[0]))
        .workloadNames(workloadNames.toArray(new String[0]))
        .applications(getEntity(BillingDataMetaDataFields.APPID, applicationIds))
        .environments(getEntity(BillingDataMetaDataFields.ENVID, environmentIds))
        .services(getEntity(BillingDataMetaDataFields.SERVICEID, serviceIds))
        .cloudProviders(cloudProviders.toArray(new String[0]));

    List<QLFilterValuesData> filterValuesDataList = new ArrayList<>();
    filterValuesDataList.add(filterValuesDataBuilder.build());
    return QLFilterValuesListData.builder().data(filterValuesDataList).build();
  }

  private List<QLEntityData> getEntity(BillingDataMetaDataFields field, Set<String> entityIds) {
    List<QLEntityData> entityData = new ArrayList<>();
    for (String entityId : entityIds) {
      entityData.add(QLEntityData.builder()
                         .name(statsHelper.getEntityName(field, entityId))
                         .id(entityId)
                         .type(field.toString())
                         .build());
    }
    return entityData;
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCCMGroupBy> groupByList, QLData qlData) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
