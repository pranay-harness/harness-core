package io.harness.ccm.billing.graphql;

import com.hazelcast.util.Preconditions;
import com.healthmarketscience.sqlbuilder.OrderObject;
import com.healthmarketscience.sqlbuilder.SqlObject;
import io.harness.ccm.billing.preaggregated.PreAggregateConstants;
import io.harness.ccm.billing.preaggregated.PreAggregatedTableSchema;
import lombok.Builder;
import lombok.Data;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;

@Data
@Builder
public class CloudSortCriteria {
  private CloudSortType sortType;
  private QLSortOrder sortOrder;

  public SqlObject toOrderObject() {
    Preconditions.checkNotNull(sortOrder, "Sort Order is missing column name.");
    if (sortType == null || sortOrder == null) {
      return null;
    }

    String orderIdentifier = null;
    switch (sortType) {
      case Time:
        orderIdentifier = PreAggregatedTableSchema.startTime.getColumnNameSQL();
        break;
      case awsBlendedCost:
        orderIdentifier = PreAggregateConstants.entityConstantAwsBlendedCost;
        break;
      case awsUnblendedCost:
        orderIdentifier = PreAggregateConstants.entityConstantAwsUnBlendedCost;
        break;
      default:
        break;
    }

    OrderObject orderObject = null;
    switch (sortOrder) {
      case ASCENDING:
        orderObject = new OrderObject(OrderObject.Dir.ASCENDING, orderIdentifier);
        break;
      case DESCENDING:
        orderObject = new OrderObject(OrderObject.Dir.DESCENDING, orderIdentifier);
        break;
      default:
        return null;
    }

    return orderObject;
  }
}
