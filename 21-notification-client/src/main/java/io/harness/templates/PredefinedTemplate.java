package io.harness.templates;

public enum PredefinedTemplate {
  EMAIL_TEST("mailtemplates/email_test.txt", "email_test"),
  SLACK_TEST("mailtemplates/slack_test.txt", "slack_test"),
  PD_TEST("mailtemplates/pd_test.txt", "pd_test"),
  MSTEAMS_TEST("mailtemplates/msteams_test.txt", "msteams_test");

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
