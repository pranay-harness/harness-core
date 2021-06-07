package software.wings.graphql.datafetcher.hackathon;

import static io.harness.annotations.dev.HarnessTeam.CE;

import static software.wings.graphql.datafetcher.billing.CloudBillingHelper.recommendations;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.billing.bigquery.BigQueryService;

import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.datafetcher.billing.CloudBillingHelper;
import software.wings.graphql.datafetcher.hackathon.QLRecommendationsOverviewData.QLRecommendationsOverviewDataBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.inject.Inject;
import com.healthmarketscience.sqlbuilder.Converter;
import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class OrphanOverviewDataFetcher
    extends AbstractObjectDataFetcher<QLRecommendationsOverviewData, QLRecommendationsParameters> {
  @Inject CloudBillingHelper cloudBillingHelper;
  @Inject BigQueryService bigQueryService;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLRecommendationsOverviewData fetch(QLRecommendationsParameters parameters, String accountId) {
    String cloudProviderTableName = cloudBillingHelper.getCloudProviderTableName(accountId, recommendations);
    BigQuery bigQuery = bigQueryService.get();
    SelectQuery query = getQuery(parameters, cloudProviderTableName);
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query.toString()).build();
    TableResult result;
    log.info("Orphan overview query: {}", query.toString());
    try {
      result = bigQuery.query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to Orphan overview. {}", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return getData(result);
  }

  private SelectQuery getQuery(QLRecommendationsParameters parameters, String cloudProviderTableName) {
    SelectQuery selectQuery = new SelectQuery();
    selectQuery.addCustomFromTable(cloudProviderTableName);

    FunctionCall functionCall = FunctionCall.sum();
    selectQuery.addCustomColumns(
        Converter.toCustomColumnSqlObject(functionCall.addCustomParams(new CustomSql("cost")), "cost"));
    Object sqlObjectFromField = new CustomSql("resourceType");
    selectQuery.addAliasedColumn(sqlObjectFromField, modifyStringToComplyRegex("resourceType"));
    selectQuery.addCustomGroupings(modifyStringToComplyRegex("resourceType"));
    return selectQuery;
  }

  private QLRecommendationsOverviewData getData(TableResult result) {
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();

    QLRecommendationsOverviewDataBuilder builder = QLRecommendationsOverviewData.builder();
    double totalCost = 0;
    for (FieldValueList row : result.iterateAll()) {
      String resourceType = "";
      double cost = 0;
      for (Field field : fields) {
        switch (field.getName()) {
          case "resourcetype":
            resourceType = row.get(field.getName()).getValue().toString();
            break;
          case "cost":
            cost = Math.round(row.get(field.getName()).getNumericValue().doubleValue() * -100D) / 100D;
            break;
          default:
            break;
        }
      }
      totalCost += cost;
      switch (resourceType) {
        case "DISK":
          builder.pdSavings(cost);
          break;
        case "INSTANCE":
          builder.vmSavings(cost);
          break;
        case "ADDRESS":
          builder.ipSavings(cost);
          break;
        default:
          break;
      }
    }
    builder.totalSavings(totalCost);
    return builder.build();
  }

  public String modifyStringToComplyRegex(String value) {
    return value.toLowerCase().replaceAll("[^a-z0-9]", "_");
  }
}