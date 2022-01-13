/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.rule;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.Builder;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;

@Builder
public class HFailOnTimeout extends Statement {
  private String testName;
  private Statement originalStatement;
  private long timeoutMs;

  private boolean lookForStuckThread;
  private volatile ThreadGroup threadGroup;

  @SuppressWarnings("PMD")
  public void evaluate() throws Throwable {
    CallableStatement callable = new CallableStatement();
    FutureTask<Throwable> task = new FutureTask(callable);
    this.threadGroup = new ThreadGroup("FailOnTimeoutGroup");
    Thread thread = new Thread(this.threadGroup, task, testName);
    thread.setDaemon(true);
    thread.start();
    callable.awaitStarted();
    Throwable throwable = this.getResult(task, thread);
    if (throwable != null) {
      throw throwable;
    }
  }

  @SuppressWarnings("PMD")
  private Throwable getResult(FutureTask<Throwable> task, Thread thread) {
    try {
      return this.timeoutMs > 0L ? task.get(this.timeoutMs, TimeUnit.MILLISECONDS) : task.get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return exception;
    } catch (ExecutionException exception) {
      return exception.getCause();
    } catch (TimeoutException exception) {
      return this.createTimeoutException(thread);
    }
  }

  private Exception createTimeoutException(Thread thread) {
    StackTraceElement[] stackTrace = thread.getStackTrace();
    Thread stuckThread = this.lookForStuckThread ? this.getStuckThread(thread) : null;
    Exception currThreadException = new TestTimedOutException(this.timeoutMs, TimeUnit.MILLISECONDS);
    if (stackTrace != null) {
      currThreadException.setStackTrace(stackTrace);
      thread.interrupt();
    }

    if (stuckThread != null) {
      Exception stuckThreadException = new Exception("Appears to be stuck in thread " + stuckThread.getName());
      stuckThreadException.setStackTrace(this.getStackTrace(stuckThread));
      return new MultipleFailureException(Arrays.asList(currThreadException, stuckThreadException));
    } else {
      return currThreadException;
    }
  }

  private StackTraceElement[] getStackTrace(Thread thread) {
    try {
      return thread.getStackTrace();
    } catch (SecurityException exception) {
      return new StackTraceElement[0];
    }
  }

  private Thread getStuckThread(Thread mainThread) {
    if (this.threadGroup == null) {
      return null;
    } else {
      Thread[] threadsInGroup = this.getThreadArray(this.threadGroup);
      if (threadsInGroup == null) {
        return null;
      } else {
        Thread stuckThread = null;
        long maxCpuTime = 0L;
        Thread[] arr$ = threadsInGroup;
        int len$ = threadsInGroup.length;

        for (int i$ = 0; i$ < len$; ++i$) {
          Thread thread = arr$[i$];
          if (thread.getState() == Thread.State.RUNNABLE) {
            long threadCpuTime = this.cpuTime(thread);
            if (stuckThread == null || threadCpuTime > maxCpuTime) {
              stuckThread = thread;
              maxCpuTime = threadCpuTime;
            }
          }
        }

        return stuckThread == mainThread ? null : stuckThread;
      }
    }
  }

  private Thread[] getThreadArray(ThreadGroup group) {
    int count = group.activeCount();
    int enumSize = Math.max(count * 2, 100);
    int loopCount = 0;

    do {
      Thread[] threads = new Thread[enumSize];
      int enumCount = group.enumerate(threads);
      if (enumCount < enumSize) {
        return this.copyThreads(threads, enumCount);
      }

      enumSize += 100;
      ++loopCount;
    } while (loopCount < 5);

    return null;
  }

  private Thread[] copyThreads(Thread[] threads, int count) {
    int length = Math.min(count, threads.length);
    Thread[] result = new Thread[length];

    if (length >= 0) {
      System.arraycopy(threads, 0, result, 0, length);
    }

    return result;
  }

  private long cpuTime(Thread thr) {
    ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
    if (mxBean.isThreadCpuTimeSupported()) {
      try {
        return mxBean.getThreadCpuTime(thr.getId());
      } catch (UnsupportedOperationException exception) {
        return 0L;
      }
    }

    return 0L;
  }

  private class CallableStatement implements Callable<Throwable> {
    private final CountDownLatch startLatch;

    private CallableStatement() {
      this.startLatch = new CountDownLatch(1);
    }

    @SuppressWarnings("PMD")
    public Throwable call() throws Exception {
      try {
        startLatch.countDown();
        originalStatement.evaluate();
        return null;
      } catch (Exception exception) {
        throw exception;
      } catch (Throwable exception) {
        return exception;
      }
    }

    public void awaitStarted() throws InterruptedException {
      this.startLatch.await();
    }
  }
}
