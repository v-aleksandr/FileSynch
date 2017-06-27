import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.stream.Stream;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * Created by Александр on 16.06.17.
 */
public class FileSynch {
    
    private static Path source;
    private static Path destination;
    
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: java FileSynch 'source directory' 'destination directory'");
            return;
        }
        verifyArgs(args[0], args[1]);
        
        Files.walkFileTree(destination, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new
                SimpleFileVisitor<Path>() {
                    
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Path targetDir = source.resolve(destination.relativize(dir));
                        if (!Files.exists(targetDir)) {
                            deleteCurrent(dir);
                        }
                        return CONTINUE;
                    }
                    
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path targetFile = destination.resolve(source.relativize(file));
                        if (!Files.exists(targetFile)) {
                            deleteCurrent(file);
                        }
                        return CONTINUE;
                    }
                    
                    private void deleteCurrent(Path currentDir) throws IOException {
                        Stream<Path> currentDirContent = Files.list(currentDir);
                        Iterator<Path> currentDirIterator = currentDirContent.iterator();
                        while (currentDirIterator.hasNext()) {
                            Path path = currentDirIterator.next();
                            if (Files.isDirectory(path)) {
                                deleteCurrent(path);
                            } else {
                                try {
                                    Files.delete(path);
                                } catch (AccessDeniedException e) {
                                    System.out.println("\tAccess denied to " + path);
                                }
                            }
                        }
                        currentDirContent.close();
                        try {
                            Files.delete(currentDir);
                        } catch (AccessDeniedException e) {
                            System.out.println("\tAccess denied to " + currentDir);
                        }
                    }

                });
        Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new
                SimpleFileVisitor<Path>() {
                    
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        Path targetdir = destination.resolve(source.relativize(dir));
                        try {
                            Files.copy(dir, targetdir);
                        } catch (FileAlreadyExistsException e) {
                            if (!Files.isDirectory(targetdir)) throw e;
                        }
                        return CONTINUE;
                    }
                    
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path newFile = destination.resolve(source.relativize(file));
                        if (!Files.exists(newFile) || Files.size(newFile) != Files.size(file)) {
                            Files.copy(file, newFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                        return CONTINUE;
                    }
                });
        System.out.println("Files copied successfully!");
    }
    
    private static void verifyArgs(String src, String dst) throws IOException {
        source = Paths.get(src);
        destination = Paths.get(dst);
        if (Files.notExists(FileSynch.source)) {
            throw new IllegalArgumentException(String.format("Source path '%s' does not exist", FileSynch.source));
        }
        if (!Files.isDirectory(FileSynch.source)) {
            throw new IllegalArgumentException(String.format("Source path %s is not a directory", FileSynch.source));
        }
        if (Files.notExists(destination) || !Files.isDirectory(destination)) {
            Files.createDirectory(destination);
            System.out.println(String.format("Created destination directory - %s", destination));
        }
    }
}
