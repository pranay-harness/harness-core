package io.harness.delegate.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static io.harness.rule.OwnerRule.JENNY;
import static org.assertj.core.api.Assertions.assertThat;

public class RollingFileAppenderTest extends CategoryTest {
    private static final String CONFIG = "src/main/resources/logback-test.xml";
    private final Logger logger = new LoggerContext().getLogger(RollingFileAppenderTest.class);
    private RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<ILoggingEvent>();
    private LoggingTriggerPolicy<ILoggingEvent> loggingTriggerPolicy = new LoggingTriggerPolicy<ILoggingEvent>();

    public void setUp() throws Exception {
    }

    @Test
    @Owner(developers = JENNY)
    @Category(UnitTests.class)
    public void createRollingPolicy_time() {
        LoggerContext loggerContext = logger.getLoggerContext();
        String file = "delegate.log";
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<ILoggingEvent>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("application");
        fileAppender.setFile(file);
        fileAppender.setAppend(true);
        SizeAndTimeBasedRollingPolicy<ILoggingEvent> policy = new SizeAndTimeBasedRollingPolicy<ILoggingEvent>();
        policy.setContext(loggerContext);
        policy.setFileNamePattern(file + ".%d{yyyy-MM-dd}");
        policy.setParent(fileAppender);
        policy.start();
        fileAppender.setRollingPolicy(policy);
        fileAppender.setTriggeringPolicy(loggingTriggerPolicy);
        fileAppender.start();
        assertThat(appender).isInstanceOf(RollingFileAppender.class);
        // assert(fileAppender.getFile().isEmpty());
        fileAppender.addInfo("message");
        assert (!fileAppender.getFile().isEmpty());
        //assert(fileAppender.getRollingPolicy().isStarted());
        LoggingTriggerPolicy.resetRollingPolicy();


    }
}
