package io.harness.delegate.service;

import static io.harness.network.SafeHttpCall.execute;

import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.managerclient.GetDelegatePropertiesRequest;
import io.harness.managerclient.GetDelegatePropertiesResponse;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.TextFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DelegatePropertyServiceImpl implements DelegatePropertyService {
  // TODO: add variable expiration time according to key
  private final LoadingCache<GetDelegatePropertiesRequest, GetDelegatePropertiesResponse> delegatePropertyCache =
      CacheBuilder.newBuilder()
          .maximumSize(5000)
          .expireAfterWrite(300, TimeUnit.MINUTES)
          .build(new CacheLoader<GetDelegatePropertiesRequest, GetDelegatePropertiesResponse>() {
            @Override
            @SneakyThrows
            public GetDelegatePropertiesResponse load(GetDelegatePropertiesRequest request) {
              return TextFormat.parse(
                  execute(delegateAgentManagerClient.getDelegateProperties(request.toString())).getResource(),
                  GetDelegatePropertiesResponse.class);
            }
          });

  private final DelegateAgentManagerClient delegateAgentManagerClient;

  @Inject
  public DelegatePropertyServiceImpl(DelegateAgentManagerClient delegateAgentManagerClient) {
    this.delegateAgentManagerClient = delegateAgentManagerClient;
  }

  @Override
  public GetDelegatePropertiesResponse getDelegateProperties(GetDelegatePropertiesRequest request)
      throws ExecutionException {
    return delegatePropertyCache.get(request);
  }

  @Override
  public void resetCache() {
    delegatePropertyCache.invalidateAll();
  }
}
