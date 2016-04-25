// package software.wings.service;
//
// import static org.assertj.core.api.Assertions.assertThat;
//
// import org.junit.Test;
// import software.wings.WingsBaseTest;
// import software.wings.beans.Host;
// import software.wings.beans.Tag;
// import software.wings.service.intfc.InfraService;
//
// import javax.inject.Inject;
//
///**
// * Created by anubhaw on 3/30/16.
// */
// public class InfraServiceTest extends WingsBaseTest {
//  @Inject private InfraService infraService;
//
//  @Test public void testCreateTag() {
//    Tag tag = new Tag();
//    tag.setType("OS");
//    tag.setName("OS");
//    tag.setDescription("Operating system types");
//    tag = infraService.createTag("ddn", tag);
//
//    Host host = new Host();
//    host = infraService.createHost("ff329r", host);
//
//    Host host1 = infraService.applyTag(host.getUuid(), tag.getUuid());
//    assertThat(host1.getTags()).hasSize(1).extracting(Tag::getUuid).containsExactly(tag.getUuid());
//  }
//}
