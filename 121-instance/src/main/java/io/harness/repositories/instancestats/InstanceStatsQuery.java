package io.harness.repositories.instancestats;

public enum InstanceStatsQuery {
  FETCH_LATEST_RECORD("SELECT * FROM INSTANCE_STATS WHERE ACCOUNTID=? ORDER BY DESC LIMIT 1");

  private String query;

  InstanceStatsQuery(String query) {
    this.query = query;
  }

  public String query() {
    return this.query;
  }
}
