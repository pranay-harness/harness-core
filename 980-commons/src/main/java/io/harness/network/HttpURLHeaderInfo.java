package io.harness.network;

import io.harness.beans.KeyValuePair;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpURLHeaderInfo {
  String url;
  List<KeyValuePair> headers;
}
