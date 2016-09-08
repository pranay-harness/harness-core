package software.wings.utils;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.Sets;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Created by peeyushaggarwal on 9/6/16.
 */
public enum FileType {
  ZIP {
    private static final long serialVersionUID = 1L;

    @Override
    protected ArchiveInputStream getArchiveInputStream(BufferedInputStream bufferedInputStream) {
      return new ZipArchiveInputStream(bufferedInputStream);
    }

    @Override
    protected String getUnarchiveCommandString(String fileName) {
      return "unzip " + fileName;
    }
  },

  TAR_GZ {
    private static final long serialVersionUID = 1L;

    @Override
    protected ArchiveInputStream getArchiveInputStream(BufferedInputStream bufferedInputStream) throws IOException {
      return new TarArchiveInputStream(new GZIPInputStream(bufferedInputStream));
    }

    @Override
    protected String getUnarchiveCommandString(String fileName) {
      return "tar xzvf " + fileName;
    }
  },

  TAR_BZ {
    private static final long serialVersionUID = 1L;

    @Override
    protected ArchiveInputStream getArchiveInputStream(BufferedInputStream bufferedInputStream) throws IOException {
      return new TarArchiveInputStream(new BZip2CompressorInputStream(bufferedInputStream));
    }

    @Override
    protected String getUnarchiveCommandString(String fileName) {
      return "tar xjvf " + fileName;
    }
  },

  TAR {
    private static final long serialVersionUID = 1L;

    @Override
    protected ArchiveInputStream getArchiveInputStream(BufferedInputStream bufferedInputStream) throws IOException {
      return new TarArchiveInputStream(bufferedInputStream);
    }

    @Override
    protected String getUnarchiveCommandString(String fileName) {
      return "tar xvf " + fileName;
    }
  },

  UNKNOWN {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean test(BufferedInputStream bufferedInputStream) {
      return true;
    }

    @Override
    public String getRoot(BufferedInputStream bufferedInputStream) {
      return ".";
    }

    @Override
    protected ArchiveInputStream getArchiveInputStream(BufferedInputStream bufferedInputStream) throws IOException {
      return null;
    }

    @Override
    protected String getUnarchiveCommandString(String fileName) {
      return "#Please write commands to unarchive " + fileName;
    }
  };

  protected static Set<String> getTopLevelHierarchy(ArchiveInputStream archiveInputStream) throws IOException {
    Set<String> topLevelElements = Sets.newHashSet();
    ArchiveEntry archiveEntry;
    while ((archiveEntry = archiveInputStream.getNextEntry()) != null) {
      Path path = Paths.get(archiveEntry.getName());
      if (path.getNameCount() > 1) {
        topLevelElements.add(path.getName(0).toString());
      } else if (archiveEntry.isDirectory()) {
        topLevelElements.add(path.getName(0).toString());
      }
    }
    return topLevelElements;
  }

  public boolean test(BufferedInputStream bufferedInputStream) {
    try {
      bufferedInputStream.mark(10 * 1024);
      return Misc.ignoreException(() -> getArchiveInputStream(bufferedInputStream).getNextEntry() != null, false);
    } finally {
      Misc.ignoreException(() -> bufferedInputStream.reset());
    }
  }

  public String getRoot(BufferedInputStream bufferedInputStream) {
    try {
      bufferedInputStream.mark(10 * 1024);
      return Misc.ignoreException(() -> {
        Set<String> topLevelHierarchy = getTopLevelHierarchy(getArchiveInputStream(bufferedInputStream));
        if (topLevelHierarchy.size() == 1) {
          return topLevelHierarchy.iterator().next();
        } else {
          return ".";
        }
      }, null);
    } finally {
      Misc.ignoreException(() -> bufferedInputStream.reset());
    }
  }

  public String getUnarchiveCommand(String fileName, String rootDirectory, String symlink) {
    return getUnarchiveCommandString(fileName) + "\n"
        + (isNotBlank(rootDirectory) ? "ln -s " + rootDirectory + " " + symlink : "");
  }

  protected abstract ArchiveInputStream getArchiveInputStream(BufferedInputStream bufferedInputStream)
      throws IOException;

  protected abstract String getUnarchiveCommandString(String fileName);
}
