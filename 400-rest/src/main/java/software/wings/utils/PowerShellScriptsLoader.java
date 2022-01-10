/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;

public class PowerShellScriptsLoader {
  public enum PsScript {
    DownloadArtifact(1, "Download Artifact"),
    ExpandArtifacts(2, "Expand Artifacts"),
    CreateIISAppPool(3, "Create AppPool"),
    CreateIISWebsite(4, "Create Website"),
    CreateIISApplication(5, "Create Application"),
    CreateIISVirtualDirectory(6, "Create Virtual Directory"),

    ;

    private final int value;
    private final String displayName;

    PsScript(int value, String displayName) {
      this.value = value;
      this.displayName = displayName;
    }

    public int getValue() {
      return this.value;
    }

    public String getDisplayName() {
      return this.displayName;
    }
  }

  private static final Map<PsScript, String> filePathMap =
      ImmutableMap.<PsScript, String>builder()
          .put(PsScript.DownloadArtifact, "powershell-scripts/Download-Artifacts.ps1")
          .put(PsScript.ExpandArtifacts, "powershell-scripts/Expand-Artifacts.ps1")
          .put(PsScript.CreateIISAppPool, "powershell-scripts/Create-IISAppPool.ps1")
          .put(PsScript.CreateIISWebsite, "powershell-scripts/Create-IISWebsite.ps1")
          .put(PsScript.CreateIISApplication, "powershell-scripts/Create-IISApplication.ps1")
          .put(PsScript.CreateIISVirtualDirectory, "powershell-scripts/Create-IISVirtualDirectory.ps1")
          .build();

  public static final Map<PsScript, String> psScriptMap;

  static {
    psScriptMap = new HashMap();
    try {
      for (Map.Entry<PsScript, String> entry : filePathMap.entrySet()) {
        InputStream inputStream = PowerShellScriptsLoader.class.getClassLoader().getResourceAsStream(entry.getValue());
        if (inputStream == null) {
          throw new RuntimeException(String.format("File '%s' not found.", entry.getValue()));
        }

        psScriptMap.put(entry.getKey(), IOUtils.toString(inputStream, StandardCharsets.UTF_8));
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to load powershell scripts.", e);
    }
  }
}
