package io.harness.ccm.views.graphql;

import com.hazelcast.util.Preconditions;
import lombok.extern.slf4j.Slf4j;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Slf4j
public class ViewsQueryHelper {
  private static final String TOTAL_COST_DATE_PATTERN = "MMM dd, yyyy";
  private static final String TOTAL_COST_DATE_PATTERN_WITHOUT_YEAR = "MMM dd";
  private static final String DEFAULT_TIME_ZONE = "GMT";
  private static final long ONE_DAY_MILLIS = 86400000;

  public boolean isYearRequired(Instant startInstant, Instant endInstant) {
    LocalDate endDate = LocalDateTime.ofInstant(endInstant, ZoneOffset.UTC).toLocalDate();
    LocalDate startDate = LocalDateTime.ofInstant(startInstant, ZoneOffset.UTC).toLocalDate();
    return startDate.getYear() - endDate.getYear() != 0;
  }

  public String getTotalCostFormattedDate(Instant instant, boolean isYearRequired) {
    if (isYearRequired) {
      return getFormattedDate(instant, TOTAL_COST_DATE_PATTERN);
    } else {
      return getFormattedDate(instant, TOTAL_COST_DATE_PATTERN_WITHOUT_YEAR);
    }
  }

  // To insert commas in given number
  public String formatNumber(Double number) {
    NumberFormat formatter = NumberFormat.getInstance(Locale.US);
    return formatter.format(number);
  }

  protected String getFormattedDate(Instant instant, String datePattern) {
    return instant.atZone(ZoneId.of(DEFAULT_TIME_ZONE)).format(DateTimeFormatter.ofPattern(datePattern));
  }

  public double getRoundedDoubleValue(double value) {
    return Math.round(value * 100D) / 100D;
  }

  public double getForecastCost(ViewCostData billingAmountData, Instant endInstant) {
    Preconditions.checkNotNull(billingAmountData);
    Instant currentTime = Instant.now();
    if (currentTime.isAfter(endInstant)) {
      return Double.valueOf(0);
    }

    long maxStartTime = getModifiedMaxStartTime(billingAmountData.getMaxStartTime());
    long billingTimeDiffMillis = ONE_DAY_MILLIS;
    if (maxStartTime != billingAmountData.getMinStartTime()) {
      billingTimeDiffMillis = maxStartTime - billingAmountData.getMinStartTime();
    }

    double totalBillingAmount = billingAmountData.getCost();
    long actualTimeDiffMillis = endInstant.toEpochMilli() - billingAmountData.getMinStartTime();
    return totalBillingAmount * (actualTimeDiffMillis / billingTimeDiffMillis);
  }

  private Long getModifiedMaxStartTime(long maxStartTime) {
    Instant instant = Instant.ofEpochMilli(maxStartTime);
    Instant dayTruncated = instant.truncatedTo(ChronoUnit.DAYS);
    Instant hourlyTruncated = instant.truncatedTo(ChronoUnit.HOURS);
    if (dayTruncated.equals(hourlyTruncated)) {
      return dayTruncated.plus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.SECONDS).toEpochMilli();
    }
    return hourlyTruncated.plus(1, ChronoUnit.HOURS).minus(1, ChronoUnit.SECONDS).toEpochMilli();
  }

  public Double getBillingTrend(
      double totalBillingAmount, double forecastCost, ViewCostData prevCostData, Instant trendFilterStartTime) {
    Double trendCostValue = 0.0;
    if (prevCostData != null && prevCostData.getCost() > 0) {
      double prevTotalBillingAmount = prevCostData.getCost();
      Instant startInstant = Instant.ofEpochMilli(prevCostData.getMinStartTime() / 1000);
      double amountDifference = totalBillingAmount - prevTotalBillingAmount;
      if (Double.valueOf(0) != forecastCost) {
        amountDifference = forecastCost - prevTotalBillingAmount;
      }
      if (trendFilterStartTime.plus(1, ChronoUnit.DAYS).isAfter(startInstant)) {
        double trendPercentage = amountDifference / prevTotalBillingAmount * 100;

        trendCostValue = getRoundedDoubleValue(trendPercentage);
      }
    }
    return trendCostValue;
  }
}
