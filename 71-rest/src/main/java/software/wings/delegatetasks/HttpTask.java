package software.wings.delegatetasks;

import static com.google.common.base.Ascii.toUpperCase;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;

import com.google.common.base.Splitter;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
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
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import software.wings.sm.states.HttpState.HttpStateExecutionResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
@Slf4j
public class HttpTask extends AbstractDelegateRunnableTask {
  private static final Splitter HEADERS_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

  private static final Splitter HEADER_SPLITTER = Splitter.on(":").trimResults();

  public HttpTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public HttpStateExecutionResponse run(TaskParameters parameters) {
    HttpTaskParameters httpTaskParameters = (HttpTaskParameters) parameters;
    return run(httpTaskParameters.getMethod(), httpTaskParameters.getUrl(), httpTaskParameters.getBody(),
        httpTaskParameters.getHeader(), httpTaskParameters.getSocketTimeoutMillis(), httpTaskParameters.isUseProxy());
  }

  @Override
  public HttpStateExecutionResponse run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  public HttpStateExecutionResponse run(
      String method, String url, String body, String headers, int socketTimeoutMillis, boolean useProxy) {
    HttpStateExecutionResponse httpStateExecutionResponse = new HttpStateExecutionResponse();

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
    HttpClientBuilder httpClientBuilder =
        HttpClients.custom().setSSLSocketFactory(sslsf).setDefaultRequestConfig(requestBuilder.build());

    if (useProxy) {
      if (Http.shouldUseNonProxy(url)) {
        throw new InvalidRequestException(
            "Delegate is configured not to use proxy for the given url: " + url, WingsException.USER);
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
        logger.warn("Task setup to use DelegateProxy but delegate setup without any proxy");
      }
    }

    CloseableHttpClient httpclient = httpClientBuilder.build();

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
      httpStateExecutionResponse.setHeader(headers);
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
