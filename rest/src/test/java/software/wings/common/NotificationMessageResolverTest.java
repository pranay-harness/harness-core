package software.wings.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.exception.WingsException;

/**
 * Created by anubhaw on 7/25/16.
 */
public class NotificationMessageResolverTest extends WingsBaseTest {
  /**
   * Should get decorated notification message.
   */
  @Test
  public void shouldGetDecoratedNotificationMessage() {
    String decoratedNotificationMessage = NotificationMessageResolver.getDecoratedNotificationMessage(
        NotificationMessageResolver.ENTITY_CREATE_NOTIFICATION,
        ImmutableMap.of("ENTITY_TYPE", "SERVICE", "ENTITY_NAME", "Account", "DATE", "July 26, 2016"));
    assertThat(decoratedNotificationMessage).isNotEmpty();
  }

  /**
   * Should fail on in complete map.
   */
  @Test
  public void shouldFailOnInCompleteMap() {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(
            ()
                -> NotificationMessageResolver.getDecoratedNotificationMessage(
                    NotificationMessageResolver.ENTITY_CREATE_NOTIFICATION, ImmutableMap.of("ENTITY_TYPE", "SERVICE")))
        .withMessage("INVALID_ARGUMENT");
  }
}
