package software.wings.delegatetasks;

import static com.google.common.base.Ascii.toUpperCase;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;

import com.google.common.base.Splitter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.states.HttpState.HttpStateExecutionResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HttpTask extends AbstractDelegateRunnableTask {
  private static final Splitter HEADERS_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

  private static final Splitter HEADER_SPLITTER = Splitter.on(":").trimResults();

  private static final Logger logger = LoggerFactory.getLogger(HttpTask.class);

  public HttpTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public HttpStateExecutionResponse run(Object[] parameters) {
    return run((String) parameters[0], (String) parameters[1], (String) parameters[2], (String) parameters[3],
        (Integer) parameters[4]);
  }

  public HttpStateExecutionResponse run(
      String method, String url, String body, String headers, int socketTimeoutMillis) {
    HttpStateExecutionResponse httpStateExecutionResponse = new HttpStateExecutionResponse();

    httpStateExecutionResponse.setHttpUrl(url);
    httpStateExecutionResponse.setHttpMethod(method);

    SSLContextBuilder builder = new SSLContextBuilder();
    try {
      builder.loadTrustMaterial((x509Certificates, s) -> true);
    } catch (NoSuchAlgorithmException | KeyStoreException e) {
      logger.error("", e);
    }
    SSLConnectionSocketFactory sslsf = null;
    try {
      sslsf = new SSLConnectionSocketFactory(builder.build(), (s, sslSession) -> true);
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      logger.error("", e);
    }

    RequestConfig.Builder requestBuilder = RequestConfig.custom();
    requestBuilder = requestBuilder.setConnectTimeout(2000);
    requestBuilder = requestBuilder.setSocketTimeout(socketTimeoutMillis);

    CloseableHttpClient httpclient =
        HttpClients.custom().setSSLSocketFactory(sslsf).setDefaultRequestConfig(requestBuilder.build()).build();

    HttpUriRequest httpUriRequest;

    switch (toUpperCase(method)) {
      case "GET":
        httpUriRequest = new HttpGet(url);
        break;
      case "POST":
        HttpPost post = new HttpPost(url);
        setEntity(body, post);
        httpUriRequest = post;
        break;
      case "PUT":
        HttpPut put = new HttpPut(url);
        setEntity(body, put);
        httpUriRequest = put;
        break;
      case "DELETE":
        httpUriRequest = new HttpDelete(url);
        break;
      case "HEAD":
      default:
        httpUriRequest = new HttpHead(url);
    }

    if (headers != null) {
      for (String header : HEADERS_SPLITTER.split(headers)) {
        List<String> headerPair = HEADER_SPLITTER.splitToList(header);

        if (headerPair.size() == 2) {
          httpUriRequest.addHeader(headerPair.get(0), headerPair.get(1));
        }
      }
    }

    httpStateExecutionResponse.setExecutionStatus(ExecutionStatus.SUCCESS);
    try {
      HttpResponse httpResponse = httpclient.execute(httpUriRequest);
      httpStateExecutionResponse.setHttpResponseCode(httpResponse.getStatusLine().getStatusCode());
      HttpEntity entity = httpResponse.getEntity();
      httpStateExecutionResponse.setHttpResponseBody(
          entity != null ? EntityUtils.toString(entity, ContentType.getOrDefault(entity).getCharset()) : "");
    } catch (IOException e) {
      logger.error("Exception occurred during HTTP task execution", e);
      httpStateExecutionResponse.setHttpResponseCode(500);
      httpStateExecutionResponse.setHttpResponseBody(getMessage(e));
      httpStateExecutionResponse.setErrorMessage(getMessage(e));
      httpStateExecutionResponse.setExecutionStatus(ExecutionStatus.ERROR);
    }

    return httpStateExecutionResponse;
  }

  private void setEntity(String body, HttpEntityEnclosingRequestBase entityEnclosingRequestBase) {
    if (body != null) {
      entityEnclosingRequestBase.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
    }
  }
}
