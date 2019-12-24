package software.wings.graphql.datafetcher.billing;

import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BillingDataHelper {
  protected double roundingDoubleFieldValue(BillingDataMetaDataFields field, ResultSet resultSet) throws SQLException {
    return Math.round(resultSet.getDouble(field.getFieldName()) * 100D) / 100D;
  }

  protected double roundingDoubleFieldPercentageValue(BillingDataMetaDataFields field, ResultSet resultSet)
      throws SQLException {
    return 100 * roundingDoubleFieldValue(field, resultSet);
  }
}
