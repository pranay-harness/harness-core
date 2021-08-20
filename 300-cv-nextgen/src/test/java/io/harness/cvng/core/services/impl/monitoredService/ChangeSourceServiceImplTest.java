package io.harness.cvng.core.services.impl.monitoredService;

import static io.harness.rule.OwnerRule.ABHIJITH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.changeSource.ChangeSource;
import io.harness.cvng.core.entities.changeSource.ChangeSource.ChangeSourceKeys;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.exception.DuplicateFieldException;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ChangeSourceServiceImplTest extends CvNextGenTestBase {
  @Inject ChangeSourceService changeSourceService;
  @Inject HPersistence hPersistence;
  BuilderFactory builderFactory;

  ServiceEnvironmentParams environmentParams;

  @Before
  public void setup() {
    builderFactory = BuilderFactory.getDefault();
    environmentParams = ServiceEnvironmentParams.builder()
                            .accountIdentifier(builderFactory.getContext().getAccountId())
                            .orgIdentifier(builderFactory.getContext().getOrgIdentifier())
                            .projectIdentifier(builderFactory.getContext().getProjectIdentifier())
                            .serviceIdentifier(builderFactory.getContext().getServiceIdentifier())
                            .environmentIdentifier(builderFactory.getContext().getProjectIdentifier())
                            .build();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testCreate() {
    ChangeSourceDTO changeSourceDTO = builderFactory.getHarnessCDChangeSourceDTOBuilder().build();
    Set<ChangeSourceDTO> changeSourceDTOToBeCreated = new HashSet<>(Arrays.asList(changeSourceDTO));

    changeSourceService.create(environmentParams, changeSourceDTOToBeCreated);

    Set<ChangeSourceDTO> changeSourceDTOSetFromDb =
        changeSourceService.get(environmentParams, Arrays.asList(changeSourceDTO.getIdentifier()));

    assertThat(changeSourceDTOSetFromDb.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testCreate_withNonUniqueIdentifier() {
    ChangeSourceDTO changeSourceDto1 = builderFactory.getHarnessCDChangeSourceDTOBuilder().build();
    ChangeSourceDTO changeSourceDto2 =
        builderFactory.getHarnessCDChangeSourceDTOBuilder().identifier(changeSourceDto1.getIdentifier()).build();
    Set<ChangeSourceDTO> changeSourceDTOToBeCreated = new HashSet<>(Arrays.asList(changeSourceDto1, changeSourceDto2));
    assertThatThrownBy(() -> changeSourceService.create(environmentParams, changeSourceDTOToBeCreated))
        .isInstanceOf(DuplicateFieldException.class);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testUpdate() {
    ChangeSourceDTO changeSourceDto = builderFactory.getHarnessCDChangeSourceDTOBuilder().build();
    Set<ChangeSourceDTO> updateDtos = new HashSet<>(Arrays.asList(changeSourceDto));
    String updatedDescription = "UPDATED_DESCRIPTION";

    changeSourceService.create(environmentParams, updateDtos);
    ChangeSource initialChangeSource = getChangeSourceFromDb(changeSourceDto.getIdentifier());

    changeSourceDto.setDescription(updatedDescription);
    changeSourceService.update(environmentParams, updateDtos);
    ChangeSource updatedChangeSource = getChangeSourceFromDb(changeSourceDto.getIdentifier());

    assertThat(updatedChangeSource.getUuid()).isEqualTo(initialChangeSource.getUuid());
    assertThat(updatedChangeSource.getCreatedAt()).isEqualTo(initialChangeSource.getCreatedAt());
    assertThat(updatedChangeSource.getDescription()).isEqualTo(updatedDescription);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testDelete() {
    ChangeSourceDTO changeSourceDto = builderFactory.getHarnessCDChangeSourceDTOBuilder().build();
    Set<ChangeSourceDTO> dtos = new HashSet<>(Arrays.asList(changeSourceDto));
    changeSourceService.create(environmentParams, dtos);

    changeSourceService.delete(environmentParams, Arrays.asList(changeSourceDto.getIdentifier()));
    ChangeSource changeSourceFromDb = getChangeSourceFromDb(changeSourceDto.getIdentifier());

    assertThat(changeSourceFromDb).isNull();
  }

  private ChangeSource getChangeSourceFromDb(String identifier) {
    return hPersistence.createQuery(ChangeSource.class).filter(ChangeSourceKeys.identifier, identifier).get();
  }
}