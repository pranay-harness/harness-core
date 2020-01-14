package software.wings.graphql.datafetcher.billing;

import io.harness.exception.InvalidRequestException;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class BillingDataHelper {
  private static final String TOTAL_COST_DATE_PATTERN = "dd MMMM, yyyy";
  private static final String DEFAULT_TIME_ZONE = "America/Los_Angeles";

  protected double roundingDoubleFieldValue(BillingDataMetaDataFields field, ResultSet resultSet) throws SQLException {
    return Math.round(resultSet.getDouble(field.getFieldName()) * 100D) / 100D;
  }

  protected double roundingDoubleFieldPercentageValue(BillingDataMetaDataFields field, ResultSet resultSet)
      throws SQLException {
    return Math.round(resultSet.getDouble(field.getFieldName()) * 10000D) / 100D;
  }

  protected String getFormattedDate(Instant instant, String datePattern) {
    return instant.atZone(ZoneId.of(DEFAULT_TIME_ZONE)).format(DateTimeFormatter.ofPattern(datePattern));
  }

  protected double getRoundedDoubleValue(BigDecimal value) {
    return Math.round(value.doubleValue() * 100D) / 100D;
  }

  protected double getRoundedDoubleValue(double value) {
    return Math.round(value * 100D) / 100D;
  }

  protected double getRoundedDoublePercentageValue(BigDecimal value) {
    return Math.round(value.doubleValue() * 10000D) / 100D;
  }

  protected QLTimeFilter getStartTimeFilter(List<QLBillingDataFilter> filters) {
    Optional<QLBillingDataFilter> startTimeDataFilter =
        filters.stream().filter(qlBillingDataFilter -> qlBillingDataFilter.getStartTime() != null).findFirst();
    if (startTimeDataFilter.isPresent()) {
      return startTimeDataFilter.get().getStartTime();
    } else {
      throw new InvalidRequestException("Start time cannot be null");
    }
  }

  protected QLTimeFilter getEndTimeFilter(List<QLBillingDataFilter> filters) {
    Optional<QLBillingDataFilter> endTimeDataFilter =
        filters.stream().filter(qlBillingDataFilter -> qlBillingDataFilter.getEndTime() != null).findFirst();
    if (endTimeDataFilter.isPresent()) {
      return endTimeDataFilter.get().getEndTime();
    } else {
      throw new InvalidRequestException("End time cannot be null");
    }
  }

  protected String getTotalCostFormattedDate(Instant instant) {
    return getFormattedDate(instant, TOTAL_COST_DATE_PATTERN);
  }
}
