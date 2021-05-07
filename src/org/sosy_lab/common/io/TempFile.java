// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.io;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Utilities for temporary files. */
public class TempFile {

  private TempFile() {}

  /** Create a builder for temporary files. */
  public static TempFileBuilder builder() {
    return new TempFileBuilder();
  }

  public static final class TempFileBuilder extends TempPathBuilder {

    private String suffix = ".tmp";
    private @Nullable Object content;
    private @Nullable Charset charset;

    private TempFileBuilder() {}

    /** Suffix of randomly generated file name, default is <code>.tmp</code>. */
    @CanIgnoreReturnValue
    public TempFileBuilder suffix(String pSuffix) {
      suffix = checkNotNull(pSuffix);
      return this;
    }

    /** Content to write to temp file immediately after creation. */
    @CanIgnoreReturnValue
    public TempFileBuilder initialContent(Object pContent, Charset pCharset) {
      checkNotNull(pContent);
      checkNotNull(pCharset);
      content = pContent;
      charset = pCharset;
      return this;
    }

    /**
     * Create a fresh temporary file according to the specifications set on this builder.
     *
     * <p>The resulting {@link Path} object is wrapped in a {@link DeleteOnCloseFile}, which deletes
     * the file as soon as {@link DeleteOnCloseFile#close()} is called.
     *
     * <p>It is recommended to use the following pattern: <code>
     * try (DeleteOnCloseFile tempFile = TempFile.builder()[.. adjust builder ..].createDeleteOnClose()) {
     *   // use tempFile.toPath() for writing and reading of the temporary file
     * }
     * </code> The file can be opened and closed multiple times, potentially from different
     * processes.
     *
     * <p>This instance can be safely used again afterwards.
     */
    @Override
    public DeleteOnCloseFile createDeleteOnClose() throws IOException {
      return new DeleteOnCloseFile(create());
    }

    @Override
    protected Path createPath(Path pDir, String pPrefix, FileAttribute<?>... pFileAttributes)
        throws IOException {
      return Files.createTempFile(pDir, pPrefix, suffix, pFileAttributes);
    }

    @Override
    protected String getPathName(Path pDir, String pPrefix) {
      return pDir.resolve(pPrefix + "*" + suffix).toString();
    }

    @Override
    protected void deletePathOnJvmExit(Path file) {
      file.toFile().deleteOnExit();
    }

    @Override
    protected void createContent(Path file) throws IOException {
      if (content != null) {
        try {
          IO.writeFile(file, charset, content);
        } catch (IOException e) {
          // creation was successful, but writing failed
          // -> delete file
          try {
            Files.delete(file);
          } catch (IOException deleteException) {
            e.addSuppressed(deleteException);
          }
          throw e;
        }
      }
    }
  }

  /**
   * A simple wrapper around {@link Path} that calls {@link Files#deleteIfExists(Path)} from {@link
   * AutoCloseable#close()}.
   */
  @SuppressWarnings("deprecation")
  @Immutable
  public static final class DeleteOnCloseFile extends MoreFiles.DeleteOnCloseFile
      implements AutoCloseable {

    private DeleteOnCloseFile(Path pFile) {
      super(pFile);
    }

    public ByteSource toByteSource() {
      return com.google.common.io.MoreFiles.asByteSource(toPath());
    }

    public ByteSink toByteSink() {
      return com.google.common.io.MoreFiles.asByteSink(toPath());
    }

    public CharSource toCharSource(Charset charset) {
      return com.google.common.io.MoreFiles.asCharSource(toPath(), charset);
    }

    public CharSink toCharSink(Charset charset) {
      return com.google.common.io.MoreFiles.asCharSink(toPath(), charset);
    }

    @Override
    @SuppressWarnings("RedundantOverride") // to avoid deprecation warning when method is called
    public Path toPath() {
      return super.toPath();
    }

    @Override
    @SuppressWarnings("RedundantOverride") // to avoid deprecation warning when method is called
    public void close() throws IOException {
      super.close();
    }
  }
}
