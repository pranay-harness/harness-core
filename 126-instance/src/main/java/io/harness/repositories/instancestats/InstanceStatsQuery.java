package io.harness.repositories.instancestats;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX)
public enum InstanceStatsQuery {
  FETCH_LATEST_RECORD("SELECT * FROM NG_INSTANCE_STATS WHERE ACCOUNTID=? ORDER BY REPORTEDAT DESC LIMIT 1");

  private String query;

  InstanceStatsQuery(String query) {
    this.query = query;
  }

  public String query() {
    return this.query;
  }
}
