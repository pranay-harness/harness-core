package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.security.access.GlobalWhitelistConfig;
import software.wings.beans.security.access.Whitelist;
import software.wings.beans.security.access.WhitelistStatus;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.WhitelistService;

import java.util.List;

/**
 *
 * @author rktummala
 */
public class WhitelistServiceTest extends WingsBaseTest {
  @Mock private AccountService accountService;
  @Mock private Account account;

  @Mock private MainConfiguration mainConfig;

  @InjectMocks @Inject private WhitelistService whitelistService;

  private String accountId = UUIDGenerator.generateUuid();
  private String whitelistId = UUIDGenerator.generateUuid();
  private String description = "whitelist 1";
  private String cidrFilter = "10.17.12.1/24";
  private String cidrFilter1 = "10.18.12.1/32";
  private String invalidCidrFilter1 = "127.0.0.1//8";
  private String invalidCidrFilter2 = "127.0.0.1/8/b";
  private String ipFilter = "192.168.0.1";
  private String invalidIpFilter1 = "999.0.0.1";
  private String invalidIpFilter2 = "127";
  private String invalidIpFilter3 = "abcd";

  /**
   * Sets mocks.
   */
  @Before
  public void setupMocks() {
    when(accountService.get(anyString())).thenReturn(account);
    when(mainConfig.getGlobalWhitelistConfig()).thenReturn(getHarnessDefaults());
  }

  private GlobalWhitelistConfig getHarnessDefaults() {
    return GlobalWhitelistConfig.builder().filters("192.168.128.0/24,127.0.0.1/8").build();
  }

  /**
   * Should save and read.
   *
   */
  @Test
  public void testSaveAndRead() {
    Whitelist whitelist =
        Whitelist.builder().accountId(accountId).uuid(whitelistId).description(description).filter(cidrFilter).build();
    Whitelist savedWhitelist = whitelistService.save(whitelist);
    compare(whitelist, savedWhitelist);

    Whitelist whitelistFromGet = whitelistService.get(accountId, whitelistId);
    compare(savedWhitelist, whitelistFromGet);

    whitelist =
        Whitelist.builder().accountId(accountId).uuid(whitelistId).description(description).filter(ipFilter).build();
    savedWhitelist = whitelistService.save(whitelist);
    compare(whitelist, savedWhitelist);

    whitelistFromGet = whitelistService.get(accountId, whitelistId);
    compare(savedWhitelist, whitelistFromGet);
  }

  @Test
  public void testList() {
    Whitelist whitelist1 = Whitelist.builder().accountId(accountId).description(description).filter(cidrFilter).build();
    Whitelist savedWhitelist1 = whitelistService.save(whitelist1);

    Whitelist whitelist2 = Whitelist.builder().accountId(accountId).description(description).filter(ipFilter).build();
    Whitelist savedWhitelist2 = whitelistService.save(whitelist2);

    PageResponse pageResponse = whitelistService.list(accountId, PageRequestBuilder.aPageRequest().build());
    assertNotNull(pageResponse);
    List<Whitelist> whitelistList = pageResponse.getResponse();
    assertThat(whitelistList).isNotNull();
    assertThat(whitelistList).hasSize(2);
    assertThat(whitelistList).containsExactlyInAnyOrder(savedWhitelist1, savedWhitelist2);
  }

  @Test
  public void testIsValidIpAddress() {
    Whitelist whitelist1 = Whitelist.builder()
                               .accountId(accountId)
                               .description(description)
                               .filter(cidrFilter)
                               .status(WhitelistStatus.ACTIVE)
                               .build();
    whitelistService.save(whitelist1);

    Whitelist whitelist2 = Whitelist.builder()
                               .accountId(accountId)
                               .description(description)
                               .filter(ipFilter)
                               .status(WhitelistStatus.ACTIVE)
                               .build();
    whitelistService.save(whitelist2);

    Whitelist whitelist3 = Whitelist.builder()
                               .accountId(accountId)
                               .description(description)
                               .filter(cidrFilter1)
                               .status(WhitelistStatus.ACTIVE)
                               .build();
    whitelistService.save(whitelist3);

    boolean valid = whitelistService.isValidIPAddress(accountId, "192.168.0.1");
    assertTrue(valid);

    valid = whitelistService.isValidIPAddress(accountId, "10.17.12.5");
    assertTrue(valid);

    valid = whitelistService.isValidIPAddress(accountId, "10.18.12.1");
    assertTrue(valid);

    valid = whitelistService.isValidIPAddress(accountId, "12.12.12.12");
    assertFalse(valid);

    valid = whitelistService.isValidIPAddress(accountId, "127.0.0.1");
    assertTrue(valid);

    valid = whitelistService.isValidIPAddress(accountId, "192.168.128.66");
    assertTrue(valid);

    valid = whitelistService.isValidIPAddress(accountId, "192.168.1.66");
    assertFalse(valid);

    valid = whitelistService.isValidIPAddress(accountId, "invalidIp");
    assertFalse(valid);
  }

  @Test
  public void testInputValidation() {
    // Negative cases
    Whitelist whitelist = Whitelist.builder()
                              .accountId(accountId)
                              .uuid(whitelistId)
                              .description(description)
                              .filter(invalidCidrFilter1)
                              .build();
    try {
      whitelistService.save(whitelist);
      assertFalse(true);
    } catch (WingsException ex) {
      assertNotNull(ex.getMessage());
    }

    whitelist = Whitelist.builder()
                    .accountId(accountId)
                    .uuid(whitelistId)
                    .description(description)
                    .filter(invalidCidrFilter2)
                    .build();
    try {
      whitelistService.save(whitelist);
      assertFalse(true);
    } catch (WingsException ex) {
      assertNotNull(ex.getMessage());
    }

    whitelist =
        Whitelist.builder().accountId(accountId).uuid(whitelistId).description(description).filter(null).build();
    try {
      whitelistService.save(whitelist);
      assertFalse(true);
    } catch (WingsException ex) {
      assertNotNull(ex.getMessage());
    }

    whitelist = Whitelist.builder()
                    .accountId(accountId)
                    .uuid(whitelistId)
                    .description(description)
                    .filter(invalidIpFilter1)
                    .build();
    try {
      whitelistService.save(whitelist);
      assertFalse(true);
    } catch (WingsException ex) {
      assertNotNull(ex.getMessage());
    }

    whitelist = Whitelist.builder()
                    .accountId(accountId)
                    .uuid(whitelistId)
                    .description(description)
                    .filter(invalidIpFilter2)
                    .build();
    try {
      whitelistService.save(whitelist);
      assertFalse(true);
    } catch (WingsException ex) {
      assertNotNull(ex.getMessage());
    }

    whitelist = Whitelist.builder()
                    .accountId(accountId)
                    .uuid(whitelistId)
                    .description(description)
                    .filter(invalidIpFilter3)
                    .build();
    try {
      whitelistService.save(whitelist);
      assertFalse(true);
    } catch (WingsException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void testUpdateAndRead() {
    Whitelist whitelist =
        Whitelist.builder().accountId(accountId).uuid(whitelistId).description(description).filter(cidrFilter).build();
    Whitelist savedWhitelist = whitelistService.save(whitelist);
    compare(whitelist, savedWhitelist);

    Whitelist whitelistFromGet = whitelistService.get(accountId, whitelistId);
    compare(savedWhitelist, whitelistFromGet);

    whitelistFromGet.setFilter(ipFilter);

    Whitelist updatedWhitelist = whitelistService.update(whitelist);
    compare(whitelist, updatedWhitelist);

    whitelistFromGet = whitelistService.get(accountId, whitelistId);
    compare(updatedWhitelist, whitelistFromGet);
  }

  @Test
  public void testUpdateWithInvalidInputs() {
    Whitelist whitelist =
        Whitelist.builder().accountId(accountId).uuid(whitelistId).description(description).filter(cidrFilter).build();
    whitelistService.save(whitelist);

    Whitelist whitelistFromGet = whitelistService.get(accountId, whitelistId);
    whitelistFromGet.setFilter(invalidCidrFilter1);
    try {
      whitelistService.update(whitelistFromGet);
      assertFalse(true);
    } catch (WingsException ex) {
      assertNotNull(ex.getMessage());
    }

    whitelistFromGet.setFilter(invalidCidrFilter2);
    try {
      whitelistService.update(whitelistFromGet);
      assertFalse(true);
    } catch (WingsException ex) {
      assertNotNull(ex.getMessage());
    }

    whitelistFromGet.setFilter(null);
    try {
      whitelistService.update(whitelistFromGet);
      assertFalse(true);
    } catch (WingsException ex) {
      assertNotNull(ex.getMessage());
    }

    whitelistFromGet.setFilter(invalidIpFilter1);
    try {
      whitelistService.update(whitelistFromGet);
      assertFalse(true);
    } catch (WingsException ex) {
      assertNotNull(ex.getMessage());
    }

    whitelistFromGet.setFilter(invalidIpFilter2);
    try {
      whitelistService.update(whitelistFromGet);
      assertFalse(true);
    } catch (WingsException ex) {
      assertNotNull(ex.getMessage());
    }

    whitelistFromGet.setFilter(invalidIpFilter3);
    try {
      whitelistService.update(whitelistFromGet);
      assertFalse(true);
    } catch (WingsException ex) {
      assertNotNull(ex.getMessage());
    }
  }

  @Test
  public void testDelete() {
    Whitelist whitelist =
        Whitelist.builder().accountId(accountId).uuid(whitelistId).description(description).filter(cidrFilter).build();
    whitelistService.save(whitelist);

    boolean delete = whitelistService.delete(accountId, whitelistId);
    assertTrue(delete);

    Whitelist whitelistAfterDelete = whitelistService.get(accountId, whitelistId);
    assertNull(whitelistAfterDelete);
  }

  private void compare(Whitelist lhs, Whitelist rhs) {
    assertEquals(lhs.getUuid(), rhs.getUuid());
    assertEquals(lhs.getDescription(), rhs.getDescription());
    assertEquals(lhs.getFilter(), rhs.getFilter());
    assertEquals(lhs.getAccountId(), rhs.getAccountId());
  }
}
