package software.wings.service.impl.yaml.directory;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.yaml.YamlConstants.MANIFEST_FILE_FOLDER;

import software.wings.beans.Service;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.service.impl.yaml.YamlManifestFileNode;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.yaml.YamlVersion.Type;
import software.wings.yaml.directory.DirectoryPath;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.directory.ServiceLevelYamlNode;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ManifestFileFolderNodeGenerator {
  @Inject private ApplicationManifestService applicationManifestService;

  public FolderNode generateManifestFileFolderNode(
      String accountId, Service service, ApplicationManifest applicationManifest, DirectoryPath manifestFilePath) {
    if (applicationManifest != null) {
      List<ManifestFile> manifestFiles =
          applicationManifestService.getManifestFilesByAppManifestId(service.getAppId(), applicationManifest.getUuid());
      return generateManifestFileFolderNode(accountId, service, manifestFiles, manifestFilePath);
    }

    return null;
  }

  public FolderNode generateManifestFileFolderNode(
      String accountId, Service service, List<ManifestFile> manifestFiles, DirectoryPath manifestFilePath) {
    FolderNode manifestFileFolder =
        new FolderNode(accountId, MANIFEST_FILE_FOLDER, ManifestFile.class, manifestFilePath, service.getAppId());

    List<YamlManifestFileNode> manifestFilesDirectUnderFiles = new ArrayList<>();
    Map<String, YamlManifestFileNode> map = new HashMap<>();

    processManifestFiles(manifestFiles, map, manifestFilesDirectUnderFiles);

    if (isNotEmpty(map)) {
      for (Map.Entry<String, YamlManifestFileNode> entry : map.entrySet()) {
        addYamlDirectoryNode(
            accountId, service.getAppId(), service.getUuid(), manifestFileFolder, entry.getValue(), manifestFilePath);
      }
    }

    manifestFilesDirectUnderFiles.forEach(yamlManifestFileNode
        -> manifestFileFolder.addChild(new ServiceLevelYamlNode(accountId, yamlManifestFileNode.getUuId(),
            service.getAppId(), service.getUuid(), yamlManifestFileNode.getName(), ManifestFile.class,
            manifestFilePath.clone().add(yamlManifestFileNode.getName()), Type.APPLICATION_MANIFEST_FILE)));

    return manifestFileFolder;
  }

  private void processManifestFiles(List<ManifestFile> manifestFiles, Map<String, YamlManifestFileNode> map,
      List<YamlManifestFileNode> fileNodesUnderFiles) {
    if (isNotEmpty(manifestFiles)) {
      sortManifestFiles(manifestFiles);

      manifestFiles.forEach(manifestFile -> {
        String name = manifestFile.getFileName();
        String[] names = name.split("/");

        if (names.length == 1) {
          fileNodesUnderFiles.add(YamlManifestFileNode.builder()
                                      .isDir(false)
                                      .name(names[0])
                                      .content(manifestFile.getFileContent())
                                      .uuId(manifestFile.getUuid())
                                      .build());
        } else {
          YamlManifestFileNode previousNode = null;
          for (int index = 0; index < names.length - 1; index++) {
            YamlManifestFileNode node = YamlManifestFileNode.builder()
                                            .isDir(true)
                                            .name(names[index])
                                            .childNodesMap(new LinkedHashMap<>())
                                            .build();

            if (previousNode == null) {
              YamlManifestFileNode startingNode = map.putIfAbsent(node.getName(), node);
              // It means it was in the map
              if (startingNode != null) {
                node = startingNode;
              }
            } else {
              previousNode.getChildNodesMap().putIfAbsent(names[index], node);
              node = previousNode.getChildNodesMap().get(names[index]);
            }

            previousNode = node;
          }

          // Add Actual File Node
          if (previousNode != null) {
            previousNode.getChildNodesMap().put(names[names.length - 1],
                YamlManifestFileNode.builder()
                    .isDir(false)
                    .name(names[names.length - 1])
                    .content(manifestFile.getFileContent())
                    .uuId(manifestFile.getUuid())
                    .build());
          }
        }
      });
    }
  }

  private void sortManifestFiles(List<ManifestFile> manifestFiles) {
    manifestFiles.sort((lhs, rhs) -> {
      String[] lhsNames = lhs.getFileName().split("/");
      String[] rhsNames = rhs.getFileName().split("/");

      if (lhsNames.length != rhsNames.length) {
        return rhsNames.length - lhsNames.length;
      }

      for (int i = 0; i < lhsNames.length; i++) {
        if (!lhsNames[i].equals(rhsNames[i])) {
          return lhsNames[i].compareTo(rhsNames[i]);
        }
      }
      return -1;
    });
  }

  private void addYamlDirectoryNode(String accountId, String appId, String serviceId, FolderNode parentFolder,
      YamlManifestFileNode node, DirectoryPath parentPath) {
    DirectoryPath directoryPath = parentPath.clone().add(node.getName());
    FolderNode direcotryFolder = new FolderNode(accountId, node.getName(), ManifestFile.class, directoryPath, appId);
    parentFolder.addChild(direcotryFolder);

    for (YamlManifestFileNode childNode : node.getChildNodesMap().values()) {
      if (childNode.isDir()) {
        addYamlDirectoryNode(accountId, appId, serviceId, direcotryFolder, childNode, directoryPath);
      } else {
        direcotryFolder.addChild(
            new ServiceLevelYamlNode(accountId, childNode.getUuId(), appId, serviceId, childNode.getName(),
                ManifestFile.class, directoryPath.clone().add(childNode.getName()), Type.APPLICATION_MANIFEST_FILE));
      }
    }
  }
}
