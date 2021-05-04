// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.io;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TempDir {

  private static final Path TMPDIR = Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value());

  private TempDir() {}

  /** Create a builder for temporary directories. */
  public static TempDirBuilder builder() {
    return new TempDirBuilder();
  }

  public static final class TempDirBuilder {

    private Path dir = TMPDIR;
    private @Nullable String prefix;
    private boolean deleteOnJvmExit = true;
    private FileAttribute<?>[] fileAttributes = new FileAttribute<?>[0];

    private TempDirBuilder() {}

    /** The directory where the temp dir will be created, default is JVM's temp directory. */
    @CanIgnoreReturnValue
    public TempDirBuilder dir(Path pDir) {
      dir = checkNotNull(pDir);
      return this;
    }

    /** Prefix of randomly-generated directory name. */
    @CanIgnoreReturnValue
    public TempDirBuilder prefix(String pPrefix) {
      prefix = checkNotNull(pPrefix);
      return this;
    }

    /** Do not automatically delete the directory including its contents on JVM exit. */
    @CanIgnoreReturnValue
    public TempDirBuilder noDeleteOnJvmExit() {
      deleteOnJvmExit = false;
      return this;
    }

    /** Use the specified {@link FileAttribute}s for creating the directory. */
    @CanIgnoreReturnValue
    public TempDirBuilder fileAttributes(FileAttribute<?>... pFileAttributes) {
      fileAttributes = pFileAttributes.clone();
      return this;
    }

    /**
     * Create a fresh temporary directory according to the specifications set on this builder.
     *
     * <p>If the temporary directory should be removed including its contents after some specific
     * code is executed, use {@link #createDeleteOnClose()}.
     *
     * <p>This instance can be safely used again afterwards.
     */
    public Path create() throws IOException {
      Path tempDir;
      try {
        tempDir = Files.createTempDirectory(dir, prefix, fileAttributes);
      } catch (IOException e) {
        // The message of this exception is often quite unhelpful,
        // improve it by adding the path where we attempted to write.
        if (e.getMessage() != null && e.getMessage().contains(dir.toString())) {
          throw e;
        }

        String fileName = dir.resolve(prefix + "*").toString();
        if (Strings.nullToEmpty(e.getMessage()).isEmpty()) {
          throw new IOException(fileName, e);
        } else {
          throw new IOException(fileName + " (" + e.getMessage() + ")", e);
        }
      }

      if (deleteOnJvmExit) {
        Runtime.getRuntime()
            .addShutdownHook(
                new Thread(
                    () -> {
                      try {
                        deleteDirectory(tempDir.toFile());
                      } catch (IOException e) {
                        // ignore, as this code is executed on JVM exit
                        // this behavior corresponds to the effect of File.deleteOnExit
                      }
                    }));
      }

      return tempDir;
    }

    /**
     * Create a fresh temporary directory according to the specifications set on this builder.
     *
     * <p>The resulting {@link Path} object is wrapped in a {@link DeleteOnCloseDir}, which deletes
     * the directory recursively including its contents as soon as {@link DeleteOnCloseDir#close()}
     * is called.
     *
     * <p>This instance can be safely used again afterwards.
     */
    public DeleteOnCloseDir createDeleteOnClose() throws IOException {
      return new DeleteOnCloseDir(create());
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
