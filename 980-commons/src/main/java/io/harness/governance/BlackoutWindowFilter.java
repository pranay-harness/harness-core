package io.harness.governance;

public interface BlackoutWindowFilter {
  BlackoutWindowFilterType getFilterType();

  void setFilterType(BlackoutWindowFilterType filterType);
}
