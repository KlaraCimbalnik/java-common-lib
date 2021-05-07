// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.io;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

public class TempDir {

  private TempDir() {}

  /** Create a builder for temporary directories. */
  public static TempDirBuilder builder() {
    return new TempDirBuilder();
  }

  public static final class TempDirBuilder extends TempPathBuilder {

    private TempDirBuilder() {}

    /**
     * Create a fresh temporary directory according to the specifications set on this builder.
     *
     * <p>The resulting {@link Path} object is wrapped in a {@link DeleteOnCloseDir}, which deletes
     * the directory recursively including its contents as soon as {@link DeleteOnCloseDir#close()}
     * is called.
     *
     * <p>It is recommended to use the following pattern: <code>
     * try (DeleteOnCloseDir tempDir = TempDir.builder()[.. adjust builder ..].createDeleteOnClose()) {
     *   // use tempDir.toPath() to get the Path object denoting the temporary directory
     * }
     * </code>
     *
     * <p>This instance can be safely used again afterwards.
     */
    @Override
    public DeleteOnCloseDir createDeleteOnClose() throws IOException {
      return new DeleteOnCloseDir(create());
    }

    @Override
    protected Path createPath(Path pDir, String pPrefix, FileAttribute<?>... pFileAttributes)
        throws IOException {
      return Files.createTempDirectory(pDir, pPrefix, pFileAttributes);
    }

    @Override
    protected String getPathName(Path pDir, String pPrefix) {
      return pDir.resolve(pPrefix + "*").toString();
    }

    @Override
    protected void deletePathOnJvmExit(Path pDir) {
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    try {
                      deleteDirectory(pDir.toFile());
                    } catch (IOException e) {
                      // ignore, as this code is executed on JVM exit
                      // this behavior corresponds to the effect of File.deleteOnExit
                    }
                  }));
    }

    @Override
    protected void createContent(Path file) throws IOException {
      // TempDir does not support initial content
    }
  }

  /**
   * A simple wrapper around {@link Path} that calls {@link Files#deleteIfExists(Path)} recursively
   * from {@link AutoCloseable#close()} to delete the directory including its contents.
   */
  @Immutable
  public static final class DeleteOnCloseDir implements AutoCloseable {

    private final Path path;

    private DeleteOnCloseDir(Path pDir) {
      path = pDir;
    }

    public Path toPath() {
      return path;
    }

    @Override
    public void close() throws IOException {
      deleteDirectory(path.toFile());
    }
  }

  @CanIgnoreReturnValue
  private static boolean deleteDirectory(File pDir) throws IOException {
    File[] contents = pDir.listFiles();
    if (contents != null) {
      for (File file : contents) {
        deleteDirectory(file);
      }
    }
    return Files.deleteIfExists(pDir.toPath());
  }
}
