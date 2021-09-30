package io.harness.wsclient;

import java.io.IOException;
import java.net.URI;

public interface DelegateWebSocketClient {
  boolean open(URI uri);
  boolean fire(String message) throws IOException;
  void close() throws IOException;
  boolean isOpen();
  void addHeader(String headerName, String headerValue);
  void setWebSocket(Object o);
}
