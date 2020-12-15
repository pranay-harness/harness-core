package io.harness.notification.templates;

public enum PredefinedTemplate {
  EMAIL_TEST("templates/email_test.txt", "email_test"),
  SLACK_TEST("templates/slack_test.txt", "slack_test"),
  SLACK_VANILLA("templates/slack_vanilla.txt", "slack_vanilla"),
  PD_TEST("templates/pd_test.txt", "pd_test"),
  MSTEAMS_TEST("templates/msteams_test.txt", "msteams_test"),
  EMAIL_TEST_WITH_USER("templates/email_test2.txt", "email_test2"),
  SLACK_TEST_WITH_USER("templates/slack_test2.txt", "slack_test2"),
  PD_TEST_WITH_USER("templates/pd_test2.txt", "pd_test2"),
  MSTEAMS_TEST_WITH_USER("templates/msteams_test2.txt", "msteams_test2");

  private String path;
  private String identifier;

  PredefinedTemplate(String path, String identifier) {
    this.path = path;
    this.identifier = identifier;
  }

  public String getPath() {
    return path;
  }

  public String getIdentifier() {
    return identifier;
  }
}
