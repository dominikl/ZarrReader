package loci.formats.services;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import dev.zarr.zarrjava.core.Array;
import dev.zarr.zarrjava.core.ArrayMetadata;
import dev.zarr.zarrjava.core.Group;
import dev.zarr.zarrjava.core.Node;
import dev.zarr.zarrjava.store.FilesystemStore;
import dev.zarr.zarrjava.store.HttpStore;
import dev.zarr.zarrjava.store.S3Store;
import dev.zarr.zarrjava.store.StoreHandle;
import dev.zarr.zarrjava.v2.Endianness;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * <p>
 * This class provides a unified interface for accessing Zarr stores from different
 * storage backends including local filesystem, HTTP/HTTPS endpoints, and S3-compatible
 * object storage. It offers convenience methods for accessing arrays and groups,
 * as well as their metadata.
 * </p>
 * <p>
 * Supported URI schemes:
 * <ul>
 * <li>file:// or no scheme - Local filesystem storage</li>
 * <li>http:// or https:// - HTTP-based storage</li>
 * <li>s3:// - S3-compatible object storage (with authentication via URL parameters)</li>
 * </ul>
 * </p>
 */
public class ZarrLocation {

    /** The underlying store handle for accessing Zarr data */
    StoreHandle store;
    
    /** The full path to the Zarr store including the .zarr extension */
    String path;
    
    /** The name of the Zarr (typically the .zarr directory name) */
    String name;

    /**
     * Constructs a ZarrLocation from the given path.
     * <p>
     * The path must contain a .zarr extension. The constructor automatically detects
     * the storage backend based on the URI scheme and initializes the appropriate store.
     * </p>
     * <p>
     * For S3 URIs, authentication parameters can be provided as query parameters:
     * <ul>
     * <li>anonymous - Use anonymous access</li>
     * <li>accessKeyId and secretAccessKey - Use basic credentials</li>
     * <li>region - Specify AWS region (defaults to US_EAST_1)</li>
     * </ul>
     * Example: s3://host/bucket/path/data.zarr?anonymous=true
     * </p>
     * 
     * @param orgPath the path to the Zarr store, must contain .zarr extension
     * @throws URISyntaxException if the path is not a valid URI
     * @throws IllegalArgumentException if the path does not contain .zarr or uses an unsupported URI scheme
     */
    public ZarrLocation(final String orgPath) throws URISyntaxException, IllegalArgumentException {
        int i = orgPath.lastIndexOf(".zarr");
        if (i > 0) {
            this.path = orgPath.substring(0, i+5);
        } else {
            throw new IllegalArgumentException("Path is not a .zarr");
        }

        URI uri = new URI(path);
        if (uri.getScheme() == null || uri.getScheme().equals("file")) {
            int sep = path.lastIndexOf(File.separator);
            name = path.substring(sep + 1);
            String storePath = path.substring(0, sep);
            store = new FilesystemStore(storePath).resolve(name);
        } else if (uri.getScheme().startsWith("http")) {
            int sep = path.lastIndexOf("/");
            name = path.substring(sep + 1);
            String storePath = path.substring(0, sep);
            store = new HttpStore(storePath).resolve(name);
        } else if (uri.getScheme().startsWith("s3")) {
            String[] tmp = path.replaceFirst("s3://", "").split("/");
            String host = tmp[0];
            String bucket = tmp[1];
            name = tmp[tmp.length - 1];
            String[] rest = Arrays.copyOfRange(tmp, 2, tmp.length);

            // Extract URL parameters for authentication
            Map<String, String> params = new HashMap<>();
            String query = (new URI(orgPath)).getQuery();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    if (idx > 0) {
                        params.put(pair.substring(0, idx), pair.substring(idx + 1));
                    }
                }
            }

            URI endpoint = new URI("https://" + host);
            S3ClientBuilder clientBuilder = S3Client.builder()
                    .endpointOverride(endpoint)
                    .region(Region.US_EAST_1); // Default region required even for non-AWS

            S3Configuration s3Config = S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build();
            clientBuilder.serviceConfiguration(s3Config);

            if (params.containsKey("anonymous")) {
                clientBuilder.credentialsProvider(AnonymousCredentialsProvider.create());
            } else if (params.containsKey("accessKeyId") && params.containsKey("secretAccessKey")) {
                AwsBasicCredentials credentials = AwsBasicCredentials.create(
                        params.get("accessKeyId"),
                        params.get("secretAccessKey"));
                clientBuilder.credentialsProvider(StaticCredentialsProvider.create(credentials));
            }
            if (params.containsKey("region")) {
                clientBuilder.region(Region.of(params.get("region")));
            }

            S3Client client = clientBuilder.build();
            store = new S3Store(client, bucket, null).resolve(rest);
        } else {
            throw new IllegalArgumentException("Unsupported URI scheme: " + uri.getScheme());
        }
    }

    /**
     * Opens and returns a Zarr group at the specified path.
     * 
     * @param path the relative path to the group within the store (empty string for root group)
     * @return the opened Group object
     * @throws IOException if the group cannot be opened or does not exist
     */
    public Group getGroup(String path) throws IOException {
        try {
            if (path.isEmpty()) {
                return (Group) Node.open(store);
            } else {
                return (Group) Node.open(store.resolve(path.split("/")));
            }
        } catch (Exception e) {
            throw new IOException("Failed to open group at path: "+store+" / " + path, e);
        }
    }

    /**
     * Opens and returns a Zarr array at the specified path.
     * 
     * @param path the relative path to the array within the store (empty string for root array)
     * @return the opened Array object
     * @throws IOException if the array cannot be opened or does not exist
     */
    public Array getArray(String path) throws IOException {
        try {
            if (path.isEmpty()) {
                return (Array) Node.open(store);
            } else {
                return (Array) Node.open(store.resolve(path.split("/")));
            }
        } catch (Exception e) {
            throw new IOException("Failed to open array at path: "+store+" / " + path, e);
        }
    }

    /**
     * Retrieves metadata from a Zarr array at the specified path.
     * 
     * @param path the relative path to the array within the store
     * @return a map containing metadata including shape, chunkShape, dataType, and littleEndian
     * @throws RuntimeException if the array cannot be accessed or metadata cannot be retrieved
     */
    public Map<String, Object> metadataFromArray(String path) {
        try {
            Array array = getArray(path);
            return metadataFromArray(array);
        } catch (IOException e) {
            throw new RuntimeException("Failed to get array metadata from path: "+store+" / " + path, e);
        }
    }

    /**
     * Extracts metadata from a Zarr array object.
     * <p>
     * The returned map contains:
     * <ul>
     * <li>shape - The dimensions of the array</li>
     * <li>chunkShape - The chunk dimensions</li>
     * <li>dataType - The data type of array elements</li>
     * <li>littleEndian - Byte order (Todo: currently always true for v3 arrays)</li>
     * </ul>
     * </p>
     * 
     * @param array the Array object to extract metadata from
     * @return a map containing the array metadata
     */
    public Map<String, Object> metadataFromArray(Array array) {
        Map<String, Object> res = new HashMap<>();
        ArrayMetadata m = array.metadata();
        if (array instanceof dev.zarr.zarrjava.v3.Array) {
            // TODO: Implement!!
            res.put("littleEndian", true);
        } else {
            dev.zarr.zarrjava.v2.ArrayMetadata m2 = (dev.zarr.zarrjava.v2.ArrayMetadata) m;
            res.put("littleEndian", m2.endianness.equals(Endianness.LITTLE));
        }
        res.put("shape", m.shape);
        res.put("chunkShape", m.chunkShape());
        res.put("dataType", m.dataType());
        return res;
    }

    /**
     * Retrieves metadata from a Zarr group at the specified path.
     * 
     * @param path the relative path to the group within the store
     * @return a map containing the group's attributes
     * @throws RuntimeException if the group cannot be accessed or metadata cannot be retrieved
     */
    public Map<String, Object> metadataFromGroup(String path) {
        try {
            Group group = getGroup(path);
            return metadataFromGroup(group);
        } catch (IOException e) {
            throw new RuntimeException("Failed to get group metadata from path: "+store+" / " + path, e);
        }
    }

    /**
     * Extracts metadata attributes from a Zarr group object.
     * <p>
     * This method handles both Zarr v2 and v3 group formats.
     * </p>
     * 
     * @param group the Group object to extract metadata from
     * @return a map containing the group's attributes
     */
    public Map<String, Object> metadataFromGroup(Group group) {
        if (group instanceof dev.zarr.zarrjava.v2.Group) {
            dev.zarr.zarrjava.v2.GroupMetadata m = ((dev.zarr.zarrjava.v2.Group) group).metadata;
            return m.attributes;
        } else {
            dev.zarr.zarrjava.v3.GroupMetadata m = ((dev.zarr.zarrjava.v3.Group) group).metadata;
            return m.attributes;
        }
    }

    /**
     * Returns the underlying store handle.
     * 
     * @return the StoreHandle for this Zarr location
     */
    public StoreHandle getStoreHandle() {
        return store;
    }

    /**
     * Returns the name of the Zarr.
     * 
     * @return the name (typically the .zarr directory name)
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the full path to the Zarr.
     * 
     * @return the complete path including the .zarr extension
     */
    public String getPath() {
        return path;
    }
}
