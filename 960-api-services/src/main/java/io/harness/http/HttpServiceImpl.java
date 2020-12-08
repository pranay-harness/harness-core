package io.harness.http;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static com.google.common.base.Ascii.toUpperCase;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.http.beans.HttpInternalConfig;
import io.harness.http.beans.HttpInternalResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.network.Http;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

@Singleton
@Slf4j
public class HttpServiceImpl implements HttpService {
  private static final Splitter HEADERS_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();
  private static final Splitter HEADER_SPLITTER = Splitter.on(":").trimResults();

  @Override
  public HttpInternalResponse executeUrl(HttpInternalConfig httpInternalConfig) {
    HttpInternalResponse httpInternalResponse = new HttpInternalResponse();

    SSLContextBuilder builder = new SSLContextBuilder();

    if (!httpInternalConfig.isCertValidationRequired()) {
      try {
        builder.loadTrustMaterial((x509Certificates, s) -> true);
      } catch (NoSuchAlgorithmException | KeyStoreException e) {
        log.error("", e);
      }
    }

    SSLConnectionSocketFactory sslsf = null;
    try {
      sslsf = new SSLConnectionSocketFactory(builder.build(), (s, sslSession) -> true);
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      log.error("", e);
    }

    RequestConfig.Builder requestBuilder = RequestConfig.custom();
    requestBuilder = requestBuilder.setConnectTimeout(2000);
    requestBuilder = requestBuilder.setSocketTimeout(httpInternalConfig.getSocketTimeoutMillis());
    HttpClientBuilder httpClientBuilder =
        HttpClients.custom().setSSLSocketFactory(sslsf).setDefaultRequestConfig(requestBuilder.build());

    if (httpInternalConfig.isUseProxy()) {
      if (Http.shouldUseNonProxy(httpInternalConfig.getUrl())) {
        throw new InvalidRequestException(
            "Delegate is configured not to use proxy for the given url: " + httpInternalConfig.getUrl(),
            WingsException.USER);
      }

      HttpHost proxyHost = Http.getHttpProxyHost();
      if (proxyHost != null) {
        if (isNotEmpty(Http.getProxyUserName())) {
          httpClientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
          BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
          credsProvider.setCredentials(new AuthScope(proxyHost),
              new UsernamePasswordCredentials(Http.getProxyUserName(), Http.getProxyPassword()));
          httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
        }
        httpClientBuilder.setProxy(proxyHost);
      } else {
        log.warn("Task setup to use DelegateProxy but delegate setup without any proxy");
      }
    }

    CloseableHttpClient httpclient = httpClientBuilder.build();

    HttpUriRequest httpUriRequest = getMethodSpecificHttpRequest(
        toUpperCase(httpInternalConfig.getMethod()), httpInternalConfig.getUrl(), httpInternalConfig.getBody());

    if (httpInternalConfig.getHeader() != null) {
      for (String header : HEADERS_SPLITTER.split(httpInternalConfig.getHeader())) {
        List<String> headerPair = HEADER_SPLITTER.splitToList(header);

        if (headerPair.size() == 2) {
          httpUriRequest.addHeader(headerPair.get(0), headerPair.get(1));
        }
      }
    }

    httpInternalResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
    try {
      HttpResponse httpResponse = httpclient.execute(httpUriRequest);
      httpInternalResponse.setHeader(httpInternalConfig.getHeader());
      httpInternalResponse.setHttpResponseCode(httpResponse.getStatusLine().getStatusCode());
      HttpEntity entity = httpResponse.getEntity();
      httpInternalResponse.setHttpResponseBody(
          entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : "");
    } catch (IOException e) {
      log.error("Exception occurred during HTTP task execution", e);
      httpInternalResponse.setHttpResponseCode(500);
      httpInternalResponse.setHttpResponseBody(getMessage(e));
      httpInternalResponse.setErrorMessage(getMessage(e));
      httpInternalResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
    }

    return httpInternalResponse;
  }

  @VisibleForTesting
  protected HttpUriRequest getMethodSpecificHttpRequest(String method, String url, String body) {
    switch (method) {
      case "GET":
        return new HttpGet(url);
      case "POST":
        HttpPost post = new HttpPost(url);
        setEntity(body, post);
        return post;
      case "PATCH":
        HttpPatch patch = new HttpPatch(url);
        setEntity(body, patch);
        return patch;
      case "PUT":
        HttpPut put = new HttpPut(url);
        setEntity(body, put);
        return put;
      case "DELETE":
        return new HttpDelete(url);
      case "HEAD":
      default:
        return new HttpHead(url);
    }
  }

  private void setEntity(String body, HttpEntityEnclosingRequestBase entityEnclosingRequestBase) {
    if (body != null) {
      entityEnclosingRequestBase.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
    }
  }
}
