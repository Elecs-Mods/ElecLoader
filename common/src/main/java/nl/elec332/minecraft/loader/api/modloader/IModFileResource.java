package nl.elec332.minecraft.loader.api.modloader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Elec332 on 06-02-2026
 * <p>
 * Used to represent {@link IModFile} contents, like JAR Entries
 */
public interface IModFileResource {

    /**
     * Opens the contents referenced by this resource as a {@link InputStream}
     *
     * @return A stream of the data referenced by this resource.
     * @throws IOException If opening the contents failed.
     */
    InputStream open() throws IOException;

    /**
     * Reads all the data referenced by this resource
     *
     * @return All the data referenced by this resource.
     * @throws IOException If reading all the data failed.
     */
    default byte[] readAllBytes() throws IOException {
        try (var stream = open()) {
            return stream.readAllBytes();
        }
    }

    /**
     * Create a copy of this resource reference that can be held onto.
     * <p>
     * Useful when using {@link IModFile#scanFile(Visitor)} where resource objects are reused and copies must be made to hold onto resources for later use.
     *
     * @return An immutable, persistent reference to the contents.
     */
    IModFileResource retain();

    @FunctionalInterface
    interface Visitor {

        /**
         * Called when a file resource is visited.
         *
         * @param relativePath The path of the file relative to the content root.
         * @param resource     A resource in the mod file. Please note that this object will be reused for the next
         *                     object when this method is called again for the same jar file, so if you need to hold
         *                     onto this object outside your visitor, use {@link IModFileResource#retain()}.
         */
        void visit(String relativePath, IModFileResource resource);

    }

}
