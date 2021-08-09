package io.harness.cvng.beans.cvnglog;

public enum CVNGLogType {
  API_CALL_LOG("ApiCallLog"),
  EXECUTION_LOG("ExecutionLog");

  private String displayName;

  CVNGLogType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}