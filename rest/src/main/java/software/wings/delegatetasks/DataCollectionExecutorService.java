package software.wings.delegatetasks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by rsingh on 5/20/18.
 */
@Singleton
public class DataCollectionExecutorService {
  private static final Logger logger = LoggerFactory.getLogger(DataCollectionExecutorService.class);

  @Inject @Named("verificationDataCollector") protected ExecutorService dataCollectionService;

  public <T> List<Optional<T>> executeParrallel(List<Callable<T>> callables) throws IOException {
    CompletionService<T> completionService = new ExecutorCompletionService<>(dataCollectionService);
    logger.info("Parallelizing callables {} ", callables.size());
    for (Callable<T> callable : callables) {
      completionService.submit(() -> {
        try {
          return callable.call();
        } catch (Exception exception) {
          logger.error("Error in executing parallel callable ", exception);
          return null;
        }
      });
    }

    List<Optional<T>> rv = new ArrayList<>();
    for (int i = 0; i < callables.size(); i++) {
      try {
        Future<T> poll = completionService.poll(3, TimeUnit.MINUTES);
        if (poll != null && poll.isDone()) {
          T result = poll.get();
          rv.add(result == null ? Optional.empty() : Optional.of(result));
        } else {
          logger.info("Timeout. Execution took longer than 3 minutes {}", callables);
          throw new TimeoutException("Timeout. Execution took longer than 3 minutes ");
        }
      } catch (Exception e) {
        throw new IOException(e);
      }
    }
    logger.info("Done parallelizing callables {} ", callables.size());
    return rv;
  }
}
