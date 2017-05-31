package software.wings.delegate.service;

import static software.wings.managerclient.SafeHttpCall.execute;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Log;
import software.wings.beans.RestResponse;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.managerclient.ManagerClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by peeyushaggarwal on 1/9/17.
 */
@Singleton
@ValidateOnExecution
public class DelegateLogServiceImpl implements DelegateLogService {
  @Inject private ManagerClient managerClient;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private Cache<String, List<Log>> cache =
      Caffeine.newBuilder()
          .expireAfterWrite(1000, TimeUnit.MILLISECONDS)
          .removalListener((String accountId, List<Log> logs, RemovalCause removalCause) -> {
            if (accountId == null || logs == null || logs.size() == 0) {
              logger.error("Unexpected Cache eviction accountId={}, logs={}", accountId, logs);
              return;
            }
            try {
              RestResponse<List<String>> restResponse = execute(managerClient.batchedSaveLogs(accountId, logs));
              logger.info("{} log lines dispatched for accountId: {}", restResponse.getResource().size(), accountId);
            } catch (IOException e) {
              e.printStackTrace();
              logger.error("Dispatch log failed. printing lost logs[{}]", logs.size());
              logs.forEach(log -> logger.error(log.toString()));
              logger.error("Finished printing lost logs");
            }
          })
          .build();

  @Override
  public void save(String accountId, Log log) {
    cache.get(accountId, s -> new ArrayList<>()).add(log);
  }
}
