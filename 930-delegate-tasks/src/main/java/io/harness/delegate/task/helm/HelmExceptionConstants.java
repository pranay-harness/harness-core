/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public class HelmExceptionConstants {
  public HelmExceptionConstants() {
    throw new UnsupportedOperationException("not supported");
  }

  public static final class Hints {
    public static final String HINT_401_UNAUTHORIZED = "Provide valid username/password for the helm repo";
    public static final String HINT_403_FORBIDDEN =
        "Make sure that the provided credentials have permissions to access the index.yaml in the helm repo. If this is a public repo, make sure it is not deprecated";
    public static final String HINT_404_HELM_REPO =
        "The given URL does not point to a valid Helm Chart Repo. Make sure to generate index.yaml using the \"helm repo index\" and upload it to the repo.";
    public static final String HINT_MALFORMED_URL =
        "Could not resolve the helm repo server URL. Please provide a reachable URL";
    public static final String HINT_MISSING_PROTOCOL_HANDLER =
        "Install protocol handler for the helm repo. For eg. If using gs://, make sure to install the Google Storage protocol support for Helm";
    public static final String DEFAULT_HINT_REPO_ADD =
        "Make sure that the repo can be added using the helm cli \"repo add\" command";

    public static final String HINT_NO_CHART_FOUND = "Provide an existing helm chart and/or valid chart path";
    public static final String HINT_NO_CHART_VERSION_FOUND = "Provide existing helm chart version";
    public static final String HINT_CHART_VERSION_IMPROPER_CONSTRAINT = "Provide existing and valid helm chart version";
    public static final String HINT_INVALID_VALUE_TYPE =
        "Could not validate value type. Please provide valid type of values.";
    public static final String DEFAULT_HINT_HELM_INSTALL =
        "Make sure that the helm chart can be installed successfully using the helm cli.";
    public static final String DEFAULT_HINT_HELM_HIST = "Helm history cmd has failed";
    public static final String DEFAULT_HINT_HELM_LIST_RELEASE = "Helm List releases cmd has failed";
    public static final String HINT_UNKNOWN_COMMAND_FLAG = "Helm incorrect command flag";

    public Hints() {
      throw new UnsupportedOperationException("not supported");
    }
  }
  public static final class Explanations {
    public static final String EXPLAIN_401_UNAUTHORIZED =
        "Given credentials are not authorized to access the helm chart repo server";
    public static final String EXPLAIN_403_FORBIDDEN = "The Helm chart repo server denied access.";
    public static final String EXPLAIN_404_HELM_REPO = "No Index.yaml file found";
    public static final String EXPLAIN_MALFORMED_URL = "The Helm chart repo server is not reachable";
    public static final String EXPLAIN_MISSING_PROTOCOL_HANDLER =
        "Protocol is not http/https. Could not find protocol handler.";
    public static final String DEFAULT_EXPLAIN_REPO_ADD = "Unable to add helm repo using the \"helm repo add command\"";

    public static final String EXPLAIN_NO_CHART_FOUND =
        "Provided chart name doesn't exist in in the chart repository or chart path is incorrect";
    public static final String EXPLAIN_NO_CHART_VERSION_FOUND =
        "Provided chart version doesn't exist in in the chart repository";
    public static final String EXPLAIN_CHART_VERSION_IMPROPER_CONSTRAINT =
        "Provided chart version doesn't match helm expected version format (e.x. https://semver.org/)";
    public static final String EXPLAIN_INVALID_VALUE_TYPE =
        "Given helm chart contains invalid type values. Please provide correct type values within the release manifest.";
    public static final String EXPLAIN_UNKNOWN_COMMAND_FLAG =
        "Provided Command flag is incorrect or invalid. Please check.";
    public static final String DEFAULT_EXPLAIN_HELM_INSTALL = "Unable to install helm chart.";
    public static final String DEFAULT_EXPLAIN_HELM_HIST = "Unable to execute release history cmd";
    public static final String DEFAULT_EXPLAIN_LIST_RELEASE = "Unable to execute list release cmd";
    public Explanations() {
      throw new UnsupportedOperationException("not supported");
    }
  }

  public static class HelmCliErrorMessages {
    public static final String NOT_FOUND_404 = "404 not found";
    public static final String UNAUTHORIZED_401 = "401 unauthorized";
    public static final String FORBIDDEN_403 = "403 forbidden";
    public static final String NO_SUCH_HOST = "no such host";
    public static final String PROTOCOL_HANDLER_MISSING = "could not find protocol handler";
    public static final String NO_CHART_FOUND = "no chart name found";
    public static final String NO_CHART_VERSION_FOUND = "no chart version found";
    public static final String CHART_VERSION_IMPROPER_CONSTRAINT = "improper constraint";
    public static final String INVALID_VALUE_TYPE = "error validating data";
    public static final String UNKNOWN_COMMAND_FLAG = "unknown flag";

    public HelmCliErrorMessages() {
      throw new UnsupportedOperationException("not supported");
    }
  }
}