package software.wings.beans.notification;

import io.harness.data.structure.CollectionUtils;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import software.wings.beans.NotificationChannelType;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

@ToString
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationSettings {
  private boolean useIndividualEmails;
  @Nonnull private Map<NotificationChannelType, List<String>> addressesByChannelType;

  public NotificationSettings(
      boolean useIndividualEmails, @Nonnull Map<NotificationChannelType, List<String>> addressesByChannelType) {
    this.useIndividualEmails = useIndividualEmails;
    this.addressesByChannelType = CollectionUtils.emptyIfNull(addressesByChannelType);
  }

  @Nonnull
  public Map<NotificationChannelType, List<String>> getAddressesByChannelType() {
    return CollectionUtils.emptyIfNull(addressesByChannelType);
  }
}
