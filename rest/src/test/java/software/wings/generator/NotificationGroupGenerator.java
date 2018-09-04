package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.generator.NotificationGroupGenerator.NotificationGroups.GENERIC_TEST;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.dl.WingsPersistence;
import software.wings.generator.AccountGenerator.Accounts;
import software.wings.generator.OwnerManager.Owners;
import software.wings.service.intfc.NotificationSetupService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sgurubelli on 8/30/18.
 */
@Singleton
public class NotificationGroupGenerator {
  @Inject AccountGenerator accountGenerator;
  @Inject WingsPersistence wingsPersistence;
  @Inject NotificationSetupService notificationSetupService;

  public enum NotificationGroups {
    GENERIC_TEST,
  }

  public NotificationGroup ensurePredefined(
      Randomizer.Seed seed, Owners owners, NotificationGroupGenerator.NotificationGroups predefined) {
    switch (predefined) {
      case GENERIC_TEST:
        return ensureGenericTest(seed, owners);
      default:
        unhandled(predefined);
    }

    return null;
  }

  private NotificationGroup ensureGenericTest(Randomizer.Seed seed, Owners owners) {
    Account account =
        owners.obtainAccount(() -> accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST));
    return ensureNotificationGroup(seed, owners,
        aNotificationGroup()
            .withName(GENERIC_TEST.name())
            .withAccountId(account.getUuid())
            .addAddressesByChannelType(
                NotificationChannelType.EMAIL, asList(System.getProperty("user.name") + "@harness.io"))
            .build());
  }

  public NotificationGroup ensureRandom(Randomizer.Seed seed, Owners owners) {
    EnhancedRandom random = Randomizer.instance(seed);
    NotificationGroupGenerator.NotificationGroups predefined =
        random.nextObject(NotificationGroupGenerator.NotificationGroups.class);
    return ensurePredefined(seed, owners, predefined);
  }

  public NotificationGroup exists(NotificationGroup notificationGroup) {
    return wingsPersistence.createQuery(NotificationGroup.class)
        .filter(Application.ACCOUNT_ID_KEY, notificationGroup.getAccountId())
        .filter(Application.NAME_KEY, notificationGroup.getName())
        .get();
  }

  public NotificationGroup ensureNotificationGroup(
      Randomizer.Seed seed, Owners owners, NotificationGroup notificationGroup) {
    EnhancedRandom random = Randomizer.instance(seed);

    final NotificationGroupBuilder builder = aNotificationGroup();

    if (notificationGroup != null && notificationGroup.getAccountId() != null) {
      builder.withAccountId(notificationGroup.getAccountId());
    } else {
      Account account = owners.obtainAccount(() -> accountGenerator.randomAccount());
      builder.withAccountId(account.getUuid());
    }

    if (notificationGroup != null && notificationGroup.getName() != null) {
      builder.withName(notificationGroup.getName());
    } else {
      builder.withName(random.nextObject(String.class));
    }

    if (notificationGroup != null && notificationGroup.isDefaultNotificationGroupForAccount()) {
      builder.withDefaultNotificationGroupForAccount(notificationGroup.isDefaultNotificationGroupForAccount());
    } else {
      builder.withDefaultNotificationGroupForAccount(true);
    }
    if (notificationGroup != null && notificationGroup.getAddressesByChannelType() != null) {
      builder.withAddressesByChannelType(notificationGroup.getAddressesByChannelType());
    } else {
      List<String> emailAddresses = new ArrayList<>();
      emailAddresses.add("engineering@harness.io");
      builder.addAddressesByChannelType(NotificationChannelType.EMAIL, emailAddresses);
    }

    NotificationGroup existing = exists(builder.build());
    if (existing != null) {
      return existing;
    }

    builder.withAppId(GLOBAL_APP_ID);

    final NotificationGroup newNotificationGroup = builder.build();

    final NotificationGroup preexisting = notificationSetupService.readNotificationGroupByName(
        newNotificationGroup.getAccountId(), newNotificationGroup.getName());
    if (preexisting != null) {
      return preexisting;
    }

    return notificationSetupService.createNotificationGroup(newNotificationGroup);
  }
}
