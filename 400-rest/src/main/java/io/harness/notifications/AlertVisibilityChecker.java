package io.harness.notifications;

import software.wings.beans.User;
import software.wings.beans.alert.Alert;

import javax.annotation.Nonnull;

/**
 * Checks if an alert should be shown to a particular user or not on the UI (under bell icon)
 */
public interface AlertVisibilityChecker {
  boolean shouldAlertBeShownToUser(String accountId, @Nonnull Alert alert, @Nonnull User user);
}
