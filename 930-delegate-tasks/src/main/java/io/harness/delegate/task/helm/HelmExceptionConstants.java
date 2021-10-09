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

    public HelmCliErrorMessages() {
      throw new UnsupportedOperationException("not supported");
    }
  }
}
