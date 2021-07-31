package io.harness.gitsync.common.events;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DX)
public interface GitSyncConfigChangeEventConstants {
  String EVENT_TYPE = "event_type";
  String CONFIG_SWITCH_TYPE = "config_switch_type";
}
