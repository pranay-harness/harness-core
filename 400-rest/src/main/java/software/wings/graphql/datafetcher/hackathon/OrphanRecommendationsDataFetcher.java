package software.wings.graphql.datafetcher.hackathon;

import static io.harness.annotations.dev.HarnessTeam.CE;

import static software.wings.graphql.datafetcher.billing.CloudBillingHelper.recommendations;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.billing.bigquery.BigQueryService;

import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.datafetcher.billing.CloudBillingHelper;
import software.wings.graphql.datafetcher.hackathon.QLRecommendationDataPoint.QLRecommendationDataPointBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import com.google.cloud.bigquery.*;
import com.google.inject.Inject;
import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.OrderObject;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class OrphanRecommendationsDataFetcher
    extends AbstractObjectDataFetcher<QLRecommendationsData, QLRecommendationsParameters> {
  @Inject CloudBillingHelper cloudBillingHelper;
  @Inject BigQueryService bigQueryService;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLRecommendationsData fetch(QLRecommendationsParameters parameters, String accountId) {
    String cloudProviderTableName = cloudBillingHelper.getCloudProviderTableName(accountId, recommendations);
    BigQuery bigQuery = bigQueryService.get();
    SelectQuery query = getQuery(parameters, cloudProviderTableName);
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query.toString()).build();
    TableResult result;
    log.info("Orphan recommendations query: {}", query.toString());
    try {
      result = bigQuery.query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to Orphan recommendations. {}", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return QLRecommendationsData.builder().data(getData(result)).build();
  }

  private SelectQuery getQuery(QLRecommendationsParameters parameters, String cloudProviderTableName) {
    SelectQuery selectQuery = new SelectQuery();
    selectQuery.addCustomFromTable(cloudProviderTableName);
    selectQuery.addAllColumns();

    if (parameters != null && !parameters.getType().equals("")) {
      Object conditionKey = new CustomSql("resourceType");
      selectQuery.addCondition(BinaryCondition.equalTo(conditionKey, parameters.getType()));
    }

    selectQuery.addCustomOrdering("cost", OrderObject.Dir.DESCENDING);

    return selectQuery;
  }

  private List<QLRecommendationDataPoint> getData(TableResult result) {
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();

    List<QLRecommendationDataPoint> dataPoints = new ArrayList<>();
    for (FieldValueList row : result.iterateAll()) {
      QLRecommendationDataPointBuilder dataPointBuilder = QLRecommendationDataPoint.builder();
      for (Field field : fields) {
        switch (field.getName()) {
          case "resource":
            dataPointBuilder.resource(row.get(field.getName()).getValue().toString());
            break;
          case "recommendationSubtype":
            dataPointBuilder.recommendationSubtype(row.get(field.getName()).getValue().toString());
            break;
          case "location":
            dataPointBuilder.location(row.get(field.getName()).getValue().toString());
            break;
          case "region":
            dataPointBuilder.region(row.get(field.getName()).getValue().toString());
            break;
          case "resourceType":
            dataPointBuilder.resourceType(row.get(field.getName()).getValue().toString());
            break;
          case "description":
            dataPointBuilder.description(row.get(field.getName()).getValue().toString());
            break;
          case "insight":
            String insight = "";
            try {
              insight = row.get(field.getName()).getValue().toString();
            } catch (Exception e) {
              log.info("Exception while fetching insights: {}", e.getMessage());
            }
            dataPointBuilder.insight(insight);
            break;
          case "currency":
            dataPointBuilder.currency(row.get(field.getName()).getValue().toString());
            break;
          case "costPeriod":
            dataPointBuilder.costPeriod(row.get(field.getName()).getValue().toString());
            break;
          case "projectId":
            dataPointBuilder.projectId(row.get(field.getName()).getValue().toString());
            break;
          case "cost":
            dataPointBuilder.cost(Math.round(row.get(field.getName()).getNumericValue().doubleValue() * -100D) / 100D);
            break;
          case "resourceLink":
            dataPointBuilder.resourceLink(row.get(field.getName()).getValue().toString());
            break;
          default:
            break;
        }
      }
      dataPoints.add(dataPointBuilder.build());
    }
    return dataPoints;
  }
}