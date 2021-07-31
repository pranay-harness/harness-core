package io.harness.gitsync.common.events;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DX)
public interface GitSyncConfigChangeEventConstants {
  public static final String EVENT_TYPE = "event_type";
  public static final String CONFIG_SWITCh_TYPE = "config_switch_type";
}
