/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.replaceEach;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;

@Singleton
@Value
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
@NonFinal
public class ExecutionConfigOverrideFromFileOnDelegate {
  static String filePath = FileUtils.getUserDirectory() + "/secret-delegate.json";
  Map<String, String> localDelegateSecrets;

  public ExecutionConfigOverrideFromFileOnDelegate() {
    Map<String, String> localSecrets = Collections.emptyMap();
    try {
      File file = FileUtils.getFile(filePath);
      if (file.exists()) {
        String jsonString = FileUtils.readFileToString(file, UTF_8);
        localSecrets = new Gson().fromJson(jsonString, new TypeToken<HashMap<String, Object>>() {}.getType());
      }
    } catch (IOException e) {
      log.info("No file found with name: {}", filePath, e);
    } catch (Exception e) {
      log.info("Failed processing file: {}", filePath, e);
    }
    localDelegateSecrets = localSecrets;
  }

  public String replacePlaceholdersWithLocalConfig(String command) {
    ArrayList<String> secretNames = new ArrayList<>(localDelegateSecrets.keySet());
    ArrayList<String> secretValues = new ArrayList<>(localDelegateSecrets.values());
    return replaceEach(command, secretNames.toArray(new String[] {}), secretValues.toArray(new String[] {}));
  }

  public boolean isLocalConfigPresent() {
    return !MapUtils.isEmpty(localDelegateSecrets);
  }
}
