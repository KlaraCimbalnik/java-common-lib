package org.sosy_lab.common.io;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class TempPathBuilder {

  private static final Path TMPDIR = Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value());

  private Path dir = TMPDIR;
  private @Nullable String prefix;
  private boolean deleteOnJvmExit = true;
  private FileAttribute<?>[] fileAttributes = new FileAttribute<?>[0];

  protected TempPathBuilder() {}

  /**
   * The directory where the temp path will be created, that is the parent path of the created temp
   * path, default is JVM's temp directory.
   */
  @CanIgnoreReturnValue
  public TempPathBuilder dir(Path pDir) {
    dir = checkNotNull(pDir);
    return this;
  }

  /**
   * Prefix of the randomly-generated name of the file or directory denoted by the created temp
   * path.
   */
  @CanIgnoreReturnValue
  public TempPathBuilder prefix(String pPrefix) {
    prefix = checkNotNull(pPrefix);
    return this;
  }

  /**
   * Do not automatically delete the file or directory denoted by the created temp path on JVM exit.
   *
   * <p>Directories are deleted including their contents.
   */
  @CanIgnoreReturnValue
  public TempPathBuilder noDeleteOnJvmExit() {
    deleteOnJvmExit = false;
    return this;
  }

  /** Use the specified {@link FileAttribute}s for creating the directory. */
  @CanIgnoreReturnValue
  public TempPathBuilder fileAttributes(FileAttribute<?>... pFileAttributes) {
    fileAttributes = pFileAttributes.clone();
    return this;
  }

  /**
   * Create a fresh temporary path according to the specifications set on this builder.
   *
   * <p>If the temporary path should be removed after some specific code is executed, use {@link
   * #createDeleteOnClose()}.
   *
   * <p>This instance can be safely used again afterwards.
   */
  public Path create() throws IOException {
    Path tempPath;
    try {
      tempPath = createPath(dir, prefix, fileAttributes);
    } catch (IOException e) {
      // The message of this exception is often quite unhelpful,
      // improve it by adding the path where we attempted to write.
      if (e.getMessage() != null && e.getMessage().contains(dir.toString())) {
        throw e;
      }

      String pathName = getPathName(dir, prefix);
      if (Strings.nullToEmpty(e.getMessage()).isEmpty()) {
        throw new IOException(pathName, e);
      } else {
        throw new IOException(pathName + " (" + e.getMessage() + ")", e);
      }
    }

    if (deleteOnJvmExit) {
      deletePathOnJvmExit(tempPath);
    }

    createContent(tempPath);

    return tempPath;
  }

  protected abstract AutoCloseable createDeleteOnClose() throws IOException;

  protected abstract Path createPath(Path pDir, String pPrefix, FileAttribute<?>... pFileAttributes)
      throws IOException;

  protected abstract String getPathName(Path pDir, String pPrefix);

  protected abstract void deletePathOnJvmExit(Path path);

  protected abstract void createContent(Path path) throws IOException;
}
