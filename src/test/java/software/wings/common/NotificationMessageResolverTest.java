package software.wings.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.exception.WingsException;

/**
 * Created by anubhaw on 7/25/16.
 */
public class NotificationMessageResolverTest extends WingsBaseTest {
  @Inject NotificationMessageResolver notificationMessageResolver;

  @Test
  public void shouldGetDecoratedNotificationMessage() {
    String decoratedNotificationMessage = notificationMessageResolver.getDecoratedNotificationMessage(
        NotificationMessageResolver.CHANGE_NOTIFICATION_TEMPLATE,
        ImmutableMap.of("URL", "http://google.com", "DATE", "July 26, 2016"));
    assertThat(decoratedNotificationMessage).isNotEmpty();
  }

  @Test
  public void shouldFailOnInCompleteMap() {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(()
                        -> notificationMessageResolver.getDecoratedNotificationMessage(
                            NotificationMessageResolver.CHANGE_NOTIFICATION_TEMPLATE,
                            ImmutableMap.of("URL", "http://google.com")))
        .withMessage("INVALID_ARGUMENT");
  }
}
