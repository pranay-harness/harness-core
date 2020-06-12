package io.harness.cvng.models;

import lombok.Value;

@Value
public class SplunkConnector extends Connector {
  String url;
  String username;
  String password;
}
