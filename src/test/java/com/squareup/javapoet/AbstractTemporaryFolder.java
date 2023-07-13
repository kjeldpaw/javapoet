package com.squareup.javapoet;

import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;

public abstract class AbstractTemporaryFolder {
    private static final String TMP_PREFIX = "junit";

    @TempDir
    File tmp;

   /** @BeforeEach
    public void setUpTemporaryFolder() throws Exception {
        tmp = createTemporaryFolder();
    }*/

   public File newFolder() throws IOException {
       return createTemporaryFolder(tmp);
   }

    public File newFolder(String path) throws IOException {
        return newFolder(new String[]{path});
    }

    /**
     * Returns a new fresh folder with the given paths under the temporary
     * folder. For example, if you pass in the strings {@code "parent"} and {@code "child"}
     * then a directory named {@code "parent"} will be created under the temporary folder
     * and a directory named {@code "child"} will be created under the newly-created
     * {@code "parent"} directory.
     */
    public File newFolder(String... paths) throws IOException {
        if (paths.length == 0) {
            throw new IllegalArgumentException("must pass at least one path");
        }

        /*
         * Before checking if the paths are absolute paths, check if create() was ever called,
         * and if it wasn't, throw IllegalStateException.
         */
        File root = tmp;
        for (String path : paths) {
            if (new File(path).isAbsolute()) {
                throw new IOException("folder path \'" + path + "\' is not a relative path");
            }
        }

        File relativePath = null;
        File file = root;
        boolean lastMkdirsCallSuccessful = true;
        for (String path : paths) {
            relativePath = new File(relativePath, path);
            file = new File(root, relativePath.getPath());

            lastMkdirsCallSuccessful = file.mkdirs();
            if (!lastMkdirsCallSuccessful && !file.isDirectory()) {
                if (file.exists()) {
                    throw new IOException(
                            "a file with the path \'" + relativePath.getPath() + "\' exists");
                } else {
                    throw new IOException(
                            "could not create a folder with the path \'" + relativePath.getPath() + "\'");
                }
            }
        }
        if (!lastMkdirsCallSuccessful) {
            throw new IOException(
                    "a folder with the path \'" + relativePath.getPath() + "\' already exists");
        }
        return file;
    }

    File createTemporaryFolder(File parentFolder) throws IOException {
        if (parentFolder != null) {
            final var parentPath = parentFolder.toPath();
            final var path = Files.createTempDirectory(parentPath, TMP_PREFIX, new FileAttribute[0]);
            return path.toFile();
        } else {
            final var path = Files.createTempDirectory(TMP_PREFIX, new FileAttribute[0]);
            return path.toFile();
        }
    }
}
