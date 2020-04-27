package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DelegateRegisterResponse {
  String delegateId;
  Action action;
  String migrateUrl;
  String sequenceNum;
  String delegateRandomToken;

  public enum Action {
    SELF_DESTRUCT("[SELF_DESTRUCT]"),
    MIGRATE("[MIGRATE]");

    private final String value;

    Action(String value) {
      this.value = value;
    }

    public String getValue() {
      return this.value;
    }
  }
}