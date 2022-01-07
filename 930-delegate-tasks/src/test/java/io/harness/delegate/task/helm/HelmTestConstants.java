/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
@OwnedBy(CDP)
public final class HelmTestConstants {
  public static final String HELM_RELEASE_NAME_KEY = "helm-release-name";
  public static final String HELM_KUBE_CONFIG_LOCATION_KEY = "helm-kube-config-location";
  public static final String GIT_FILE_CONTENT_1_KEY = "git-file-content-1";
  public static final String GIT_FILE_CONTENT_2_KEY = "git-file-content-2";
  public static final String GIT_FILE_CONTENT_3_KEY = "git-file-content-3";
  public static final String FILE_PATH_KEY = "file/Path";
  public static final String CHART_NAME_KEY = "chart-name";
  public static final String LIST_RELEASE_V2 =
      "NAME                                     \tREVISION\tUPDATED                 \tSTATUS  \tCHART         \tNAMESPACE\n"
      + "helm-release-name\t85      \tThu Aug  9 23:19:57 2018\tDEPLOYED\ttodolist-0.1.0\tdefault  ";
  public static final String LIST_RELEASE_V3 =
      "NAME                                    \tNAMESPACE\tREVISION\tUPDATED                                \tSTATUS  \tCHART                 \tAPP VERSION\n"
      + "ft-test                                 \tdefault  \t1       \t2020-01-22 13:09:28.485806 +0530 IST   \tfailed  \taks-helloworld-0.1.0\n"
      + "ft-test1                                \tharness  \t1       \t2020-01-22 14:34:30.181647 +0530 IST   \tdeployed\taks-helloworld-0.1.0\n"
      + "helm2-http                              \tdefault  \t6       \t2020-01-22 12:09:51.580197617 +0000 UTC\tdeployed\taks-helloworld-0.1.0\n";
  public static final String RELEASE_HIST_V2 =
      "REVISION\tUPDATED                 \tSTATUS    \tCHART            \tAPP VERSION\tDESCRIPTION\n"
      + "1       \tSun Jan 19 16:43:18 2020\tSUPERSEDED\tchartmuseum-2.3.1\t0.8.2      \tInstall complete\n"
      + "2       \tSun Jan 19 22:09:02 2020\tSUPERSEDED\tchartmuseum-2.3.2\t0.8.2      \tUpgrade complete\n"
      + "3       \tSun Jan 19 22:14:59 2020\tSUPERSEDED\tchartmuseum-2.3.3\t0.8.2      \tUpgrade complete\n"
      + "4       \tMon Jan 20 10:23:01 2020\tSUPERSEDED\tchartmuseum-2.3.4\t0.8.2      \tUpgrade complete\n"
      + "5       \tMon Jan 20 10:59:36 2020\tSUPERSEDED\tchartmuseum-2.3.5\t0.8.2      \tUpgrade complete";
  public static final String RELEASE_HIST_V3 =
      "REVISION\tUPDATED                 \tSTATUS    \tCHART            \tAPP VERSION\tDESCRIPTION\n"
      + "1       \tWed Jan 22 10:37:39 2020\tsuperseded\tzetcd-0.1.4      \t0.0.3      \tInstall complete\n"
      + "2       \tWed Jan 22 11:09:34 2020\tsuperseded\tzetcd-0.1.9      \t0.0.3      \tRollback to 1\n"
      + "3       \tWed Jan 22 11:30:37 2020\tsuperseded\tzetcd-0.2.9      \t0.0.3      \tRollback to 2\n"
      + "4       \tWed Jan 22 11:34:51 2020\tsuperseded\tchartmuseum-2.7.0\t0.11.0     \tUpgrade complete";
  public static final String VERSION_V2 = "Client: v2.16.0+ge13bc94\n"
      + "Server: v2.16.0+ge13bc94";
  public static final String VERSION_V3 = "v3.1.2+g19e47ee";
  public static final String REPO_LIST_V3 = "NAME                                \tURL\n"
      + "bitnami                             \thttps://charts.bitnami.com/bitnami\n"
      + "oci                                 \thttp://127.0.0.1:8080\n"
      + "abc                                 \thttps://yogesh-test.storage.googleapis.com/helm3-charts";
  public static final String REPO_LIST_V2 = "NAME        \tURL\n"
      + "vmware-tanzu\thttps://vmware-tanzu.github.io/helm-charts\n"
      + "jfrog-helm  \thttps://harness.jfrog.io/harness/helm";
  public static final String VALID_VALUES_YAML = "# Enter your Helm value YAML\n"
      + "#\n"
      + "# Placeholders:\n"
      + "#\n"
      + "# Optional: ${NAMESPACE}\n"
      + "#   - Replaced with the namespace\n"
      + "#     Harness will set the namespace from infrastructure\n"
      + "#     mapping namespace\n"
      + "#\n"
      + "# Optional: ${DOCKER_IMAGE_NAME}\n"
      + "#   - Replaced with the Docker image name\n"
      + "#\n"
      + "# Optional: ${DOCKER_IMAGE_TAG}\n"
      + "#   - Replaced with the Docker image tag\n"
      + "#\n"
      + "# ---\n"
      + "namespace : ${NAMESPACE}\n";

  public static final String INVALID_VALUES_YAML = "I-miss-my-namespace";

  private HelmTestConstants() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }
}
