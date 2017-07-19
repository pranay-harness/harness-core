package software.wings.delegatetasks;

import static com.google.common.base.Ascii.toUpperCase;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static software.wings.api.HttpStateExecutionData.Builder.aHttpStateExecutionData;

import com.google.common.base.Splitter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
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
import software.wings.api.HttpStateExecutionData;
import software.wings.beans.DelegateTask;
import software.wings.sm.ExecutionStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by peeyushaggarwal on 12/7/16.
 */
public class HttpTask extends AbstractDelegateRunnableTask<HttpStateExecutionData> {
  private static final Splitter HEADERS_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

  private static final Splitter HEADER_SPLITTER = Splitter.on(":").trimResults();

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public HttpTask(String delegateId, DelegateTask delegateTask, Consumer<HttpStateExecutionData> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public HttpStateExecutionData run(Object[] parameters) {
    return run((String) parameters[0], (String) parameters[1], (String) parameters[2], (String) parameters[3],
        (Integer) parameters[4]);
  }

  public HttpStateExecutionData run(String method, String url, String body, String headers, int socketTimeoutMillis) {
    HttpStateExecutionData.Builder executionDataBuilder =
        aHttpStateExecutionData().withHttpUrl(url).withHttpMethod(method);

    SSLContextBuilder builder = new SSLContextBuilder();
    try {
      builder.loadTrustMaterial((x509Certificates, s) -> true);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (KeyStoreException e) {
      e.printStackTrace();
    }
    SSLConnectionSocketFactory sslsf = null;
    try {
      sslsf = new SSLConnectionSocketFactory(builder.build(), (s, sslSession) -> true);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (KeyManagementException e) {
      e.printStackTrace();
    }

    RequestConfig.Builder requestBuilder = RequestConfig.custom();
    requestBuilder = requestBuilder.setConnectTimeout(2000);
    requestBuilder = requestBuilder.setSocketTimeout(socketTimeoutMillis);

    CloseableHttpClient httpclient =
        HttpClients.custom().setSSLSocketFactory(sslsf).setDefaultRequestConfig(requestBuilder.build()).build();

    HttpUriRequest httpUriRequest = null;

    switch (toUpperCase(method)) {
      case "GET": {
        httpUriRequest = new HttpGet(url);
        break;
      }
      case "POST": {
        HttpPost post = new HttpPost(url);
        if (body != null) {
          post.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
        }
        httpUriRequest = post;
        break;
      }
      case "PUT": {
        HttpPut put = new HttpPut(url);
        if (body != null) {
          put.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
        }
        httpUriRequest = put;
        break;
      }
      case "DELETE": {
        httpUriRequest = new HttpDelete(url);
        break;
      }
      case "HEAD": {
        httpUriRequest = new HttpHead(url);
        break;
      }
    }

    if (headers != null) {
      for (String header : HEADERS_SPLITTER.split(headers)) {
        List<String> headerPair = HEADER_SPLITTER.splitToList(header);

        if (headerPair.size() == 2) {
          httpUriRequest.addHeader(headerPair.get(0), headerPair.get(1));
        }
      }
    }

    executionDataBuilder.withStatus(ExecutionStatus.SUCCESS);
    try {
      HttpResponse httpResponse = httpclient.execute(httpUriRequest);
      executionDataBuilder.withHttpResponseCode(httpResponse.getStatusLine().getStatusCode());
      HttpEntity entity = httpResponse.getEntity();
      executionDataBuilder.withHttpResponseBody(
          entity != null ? EntityUtils.toString(entity, ContentType.getOrDefault(entity).getCharset()) : "");
    } catch (IOException e) {
      logger.error("Exception occurred during HTTP task execution: " + e.getMessage(), e);
      Arrays.stream(e.getStackTrace()).forEach(elem -> logger.error("Trace: {}", elem));
      executionDataBuilder.withHttpResponseCode(500)
          .withHttpResponseBody(getMessage(e))
          .withErrorMsg(getMessage(e))
          .withStatus(ExecutionStatus.ERROR);
    }

    return executionDataBuilder.build();
  }

  public static final class Builder {
    private String delegateId;
    private DelegateTask delegateTask;
    private Consumer<HttpStateExecutionData> postExecute;
    private Supplier<Boolean> preExecute;

    private Builder() {}

    public static Builder aHttpTask() {
      return new Builder();
    }

    public Builder withDelegateId(String delegateId) {
      this.delegateId = delegateId;
      return this;
    }

    public Builder withDelegateTask(DelegateTask delegateTask) {
      this.delegateTask = delegateTask;
      return this;
    }

    public Builder withPostExecute(Consumer<HttpStateExecutionData> postExecute) {
      this.postExecute = postExecute;
      return this;
    }

    public Builder withPreExecute(Supplier<Boolean> preExecute) {
      this.preExecute = preExecute;
      return this;
    }

    public HttpTask build() {
      HttpTask httpTask = new HttpTask(delegateId, delegateTask, postExecute, preExecute);
      return httpTask;
    }
  }
}
