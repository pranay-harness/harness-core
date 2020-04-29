package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.HarnessTagType;
import software.wings.dl.WingsPersistence;

/**
 * Add TagType To Harness Tag and TagLink classes.
 * @author rktummala on 04/28/20
 */
@Slf4j
public class AddTagTypeToTagAndTagLinkMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (HIterator<Account> accounts =
             new HIterator<>(wingsPersistence.createQuery(Account.class).project(Account.ID_KEY, true).fetch())) {
      while (accounts.hasNext()) {
        final Account account = accounts.next();
        try (HIterator<HarnessTag> tags = new HIterator<>(
                 wingsPersistence.createQuery(HarnessTag.class).filter("accountId", account.getUuid()).fetch())) {
          while (tags.hasNext()) {
            HarnessTag tag = tags.next();
            wingsPersistence.updateField(HarnessTag.class, tag.getUuid(), "tagType", HarnessTagType.USER);
          }
        }

        try (HIterator<HarnessTagLink> tagLinks = new HIterator<>(
                 wingsPersistence.createQuery(HarnessTagLink.class).filter("accountId", account.getUuid()).fetch())) {
          while (tagLinks.hasNext()) {
            HarnessTagLink tagLink = tagLinks.next();
            wingsPersistence.updateField(HarnessTagLink.class, tagLink.getUuid(), "tagType", HarnessTagType.USER);
          }
        }
      }
    }
  }
}
