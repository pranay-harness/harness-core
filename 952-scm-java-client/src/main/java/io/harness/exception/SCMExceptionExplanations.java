package io.harness.exception;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SCMExceptionExplanations {
  public static final String UNABLE_TO_PUSH_TO_REPO_WITH_USER_CREDENTIALS =
      "We could not perform a push to the git repository using the credentials of the user. Possible reasons can be:\n"
      + "1. Missing permissions to push to the selected branch of the repo\n"
      + "2. Repo does not exist or has been deleted\n"
      + "3. Credentials have expired";
}
