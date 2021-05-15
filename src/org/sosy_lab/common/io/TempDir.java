// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.io;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.MoreFiles.deleteRecursively;
import static org.sosy_lab.common.io.TempFile.TMPDIR;

import com.google.common.base.Strings;
import com.google.common.io.MoreFiles;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TempDir {

  private TempDir() {}

  /** Create a builder for temporary directories. */
  public static TempDirBuilder builder() {
    return new TempDirBuilder();
  }

  public static final class TempDirBuilder {

    private Path dir = TMPDIR;
    private @Nullable String prefix;
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

        String dirName = dir.resolve(prefix + "*").toString();
        if (Strings.nullToEmpty(e.getMessage()).isEmpty()) {
          throw new IOException(dirName, e);
        } else {
          throw new IOException(dirName + " (" + e.getMessage() + ")", e);
        }
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
     * <p>It is recommended to use the following pattern: <code>
     * try (DeleteOnCloseDir tempDir = TempDir.builder()[.. adjust builder ..].createDeleteOnClose()) {
     *   // use tempDir.toPath() to get the Path object denoting the temporary directory
     * }
     * </code>
     *
     * <p>This instance can be safely used again afterwards.
     */
    public DeleteOnCloseDir createDeleteOnClose() throws IOException {
      return new DeleteOnCloseDir(create());
    }
  }

  /**
   * A simple wrapper around {@link Path} that calls {@link MoreFiles#deleteRecursively(Path,
   * com.google.common.io.RecursiveDeleteOption...)} recursively from {@link AutoCloseable#close()}
   * to delete the directory including its contents.
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
      deleteRecursively(path);
    }
  }
}
