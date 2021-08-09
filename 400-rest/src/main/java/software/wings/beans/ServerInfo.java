package software.wings.beans;

import java.time.ZoneId;

/**
 * Created by anubhaw on 10/19/16.
 */
public class ServerInfo {
  private ZoneId zoneId;

  public ZoneId getZoneId() {
    return zoneId;
  }

  public void setZoneId(ZoneId zoneId) {
    this.zoneId = zoneId;
  }
}
