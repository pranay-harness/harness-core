package io.harness.cvng.models;

import java.util.Map;

public interface Connector {
  String getAccountId();
  String getBaseUrl();
  Map<String, String> collectionHeaders();
  Map<String, String> collectionParams();
}
