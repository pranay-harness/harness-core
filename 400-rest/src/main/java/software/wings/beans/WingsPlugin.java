/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import software.wings.settings.SettingValue;

import com.github.zafarkhaja.semver.Version;
import java.util.List;

/**
 * Created by peeyushaggarwal on 10/20/16.
 */
public interface WingsPlugin {
  String getType();
  Class<? extends SettingValue> getSettingClass();
  List<PluginCategory> getPluginCategories();
  boolean isEnabled();
  Version getVersion();
}
