package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcher;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLEntityTableData;
import software.wings.graphql.schema.type.aggregation.billing.QLEntityTableData.QLEntityTableDataBuilder;
import software.wings.graphql.schema.type.aggregation.billing.QLEntityTableListData;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

@Slf4j
public class BillingStatsEntityDataFetcher extends AbstractStatsDataFetcher<QLCCMAggregationFunction,
    QLBillingDataFilter, QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject QLBillingStatsHelper statsHelper;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, QLCCMAggregationFunction aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sortCriteria) {
    try {
      if (timeScaleDBService.isValid()) {
        return getEntityData(accountId, filters, aggregateFunction, groupBy, sortCriteria);
      } else {
        throw new InvalidRequestException("Cannot process request in BillingStatsEntityDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching billing data {}", e);
    }
  }

  protected QLEntityTableListData getEntityData(@NotNull String accountId, List<QLBillingDataFilter> filters,
      QLCCMAggregationFunction aggregateFunction, List<QLCCMGroupBy> groupByList,
      List<QLBillingSortCriteria> sortCriteria) {
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupByList);

    queryData =
        billingDataQueryBuilder.formQuery(accountId, filters, aggregateFunction, groupByEntityList, sortCriteria);
    logger.info("BillingStatsTimeSeriesDataFetcher query!! {}", queryData.getQuery());
    logger.info(queryData.getQuery());

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      return generateEntityData(queryData, resultSet);
    } catch (SQLException e) {
      logger.error("BillingStatsTimeSeriesDataFetcher Error exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return null;
  }

  private QLEntityTableListData generateEntityData(BillingDataQueryMetadata queryData, ResultSet resultSet)
      throws SQLException {
    List<QLEntityTableData> entityTableListData = new ArrayList<>();
    while (resultSet != null && resultSet.next()) {
      String entityId = BillingStatsDefaultKeys.ENTITYID;
      String type = BillingStatsDefaultKeys.TYPE;
      String name = BillingStatsDefaultKeys.NAME;
      Double totalCost = BillingStatsDefaultKeys.TOTALCOST;
      Double idleCost = BillingStatsDefaultKeys.IDLECOST;
      Double costTrend = BillingStatsDefaultKeys.COSTTREND;
      String trendType = BillingStatsDefaultKeys.TRENDTYPE;
      String region = BillingStatsDefaultKeys.REGION;

      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        if (field.equals(BillingDataMetaDataFields.APPID) || field.equals(BillingDataMetaDataFields.ENVID)
            || field.equals(BillingDataMetaDataFields.SERVICEID) || field.equals(BillingDataMetaDataFields.CLUSTERID)) {
          type = field.getFieldName();
          entityId = resultSet.getString(field.getFieldName());
          name = statsHelper.getEntityName(field, entityId);
        }

        if (field.equals(BillingDataMetaDataFields.REGION)) {
          region = resultSet.getString(field.getFieldName());
        }

        if (field.equals(BillingDataMetaDataFields.SUM)) {
          totalCost = Math.round(resultSet.getDouble(field.getFieldName()) * 100D) / 100D;
        }
      }

      final QLEntityTableDataBuilder entityTableDataBuilder = QLEntityTableData.builder();
      entityTableDataBuilder.id(entityId)
          .name(name)
          .type(type)
          .totalCost(totalCost)
          .idleCost(idleCost)
          .costTrend(costTrend)
          .trendType(trendType)
          .region(region);

      entityTableListData.add(entityTableDataBuilder.build());
    }

    return QLEntityTableListData.builder().data(entityTableListData).build();
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
