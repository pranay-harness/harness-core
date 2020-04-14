package io.harness.commandlibrary.client;

import static io.harness.commandlibrary.common.CommandLibraryConstants.MANAGER_CLIENT_ID;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.harness.commandlibrary.common.service.CommandLibraryService;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.network.Http;
import io.harness.security.ServiceTokenGenerator;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class CommandLibraryServiceHttpClientFactory implements Provider<CommandLibraryServiceHttpClient> {
  private final String baseUrl;
  private final ServiceTokenGenerator tokenGenerator;
  @Inject private CommandLibraryService commandLibraryService;

  public CommandLibraryServiceHttpClientFactory(String baseUrl, ServiceTokenGenerator tokenGenerator) {
    this.baseUrl = baseUrl;
    this.tokenGenerator = tokenGenerator;
  }

  @Override
  public CommandLibraryServiceHttpClient get() {
    logger.info(" Create Command Library service retrofit client");
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new JavaTimeModule());
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(baseUrl)
                                  .client(getUnsafeOkHttpClient(baseUrl))
                                  .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                                  .build();
    return retrofit.create(CommandLibraryServiceHttpClient.class);
  }

  private OkHttpClient getUnsafeOkHttpClient(String baseUrl) {
    try {
      return Http.getUnsafeOkHttpClientBuilder(baseUrl, 15, 15)
          .connectionPool(new ConnectionPool())
          .retryOnConnectionFailure(true)
          .addInterceptor(getAuthorizationInterceptor())
          .build();

    } catch (Exception e) {
      throw new GeneralException("error while creating okhttp client for Command library service", e);
    }
  }

  @NotNull
  private Interceptor getAuthorizationInterceptor() {
    final Supplier<String> secretKeyForManageSupplier =
        Suppliers.memoizeWithExpiration(this ::getServiceSecretForManager, 1, TimeUnit.MINUTES);
    return chain -> {
      String token = tokenGenerator.getServiceToken(secretKeyForManageSupplier.get());
      Request request = chain.request();
      return chain.proceed(
          request.newBuilder().header("Authorization", MANAGER_CLIENT_ID + StringUtils.SPACE + token).build());
    };
  }

  @VisibleForTesting
  String getServiceSecretForManager() {
    final String secretForClient = commandLibraryService.getSecretForClient(MANAGER_CLIENT_ID);
    if (StringUtils.isBlank(secretForClient)) {
      throw new InvalidRequestException("no secret key for client : " + MANAGER_CLIENT_ID);
    }
    return secretForClient;
  }
}
