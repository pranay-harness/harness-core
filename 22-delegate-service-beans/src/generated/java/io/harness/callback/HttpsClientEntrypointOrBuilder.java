// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/callback/http_entrypoint.proto

package io.harness.callback;

@javax.annotation.Generated(value = "protoc", comments = "annotations:HttpsClientEntrypointOrBuilder.java.pb.meta")
public interface HttpsClientEntrypointOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.callback.HttpsClientEntrypoint)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>string url = 1[json_name = "url"];</code>
   * @return The url.
   */
  java.lang.String getUrl();
  /**
   * <code>string url = 1[json_name = "url"];</code>
   * @return The bytes for url.
   */
  com.google.protobuf.ByteString getUrlBytes();

  /**
   * <code>.io.harness.callback.BasicAuthCredentials basic_auth_credentials = 2[json_name =
   * "basicAuthCredentials"];</code>
   * @return Whether the basicAuthCredentials field is set.
   */
  boolean hasBasicAuthCredentials();
  /**
   * <code>.io.harness.callback.BasicAuthCredentials basic_auth_credentials = 2[json_name =
   * "basicAuthCredentials"];</code>
   * @return The basicAuthCredentials.
   */
  io.harness.callback.BasicAuthCredentials getBasicAuthCredentials();
  /**
   * <code>.io.harness.callback.BasicAuthCredentials basic_auth_credentials = 2[json_name =
   * "basicAuthCredentials"];</code>
   */
  io.harness.callback.BasicAuthCredentialsOrBuilder getBasicAuthCredentialsOrBuilder();

  public io.harness.callback.HttpsClientEntrypoint.CredentialsCase getCredentialsCase();
}
