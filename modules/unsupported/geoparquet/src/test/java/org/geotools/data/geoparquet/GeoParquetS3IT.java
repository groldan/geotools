/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2025, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.geoparquet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.apache.commons.io.FileUtils;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.filter.Filter;
import org.geotools.filter.FilterFactoryImpl;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** Tests for GeoParquet DataStore with S3 access. */
public class GeoParquetS3IT extends GeoParquetTestBase {

    private static final FilterFactoryImpl FF = new FilterFactoryImpl();
    private static final String BUCKET_NAME = "geoparquet-test-bucket";

    /** The resource directory containing test data */
    protected static final String TEST_DATA_DIR = "/test-data/overturemaps";

    @SuppressWarnings("resource")
    @ClassRule
    public static LocalStackContainer localstack = new LocalStackContainer(
                    DockerImageName.parse("localstack/localstack"))
            .withServices(LocalStackContainer.Service.S3);

    private static S3Client s3Client;
    private static URI endpoint;

    @BeforeClass
    public static void setupS3() throws IOException {
        // Create S3 client

        String accessKey = localstack.getAccessKey();
        String secretKey = localstack.getSecretKey();
        AwsBasicCredentials credentials = AwsBasicCredentials.builder()
                .accessKeyId(accessKey)
                .secretAccessKey(secretKey)
                .build();

        endpoint = localstack.getEndpoint();

        s3Client = S3Client.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("us-east-1"))
                .build();

        // Create test bucket
        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());

        // Upload test files to S3
        //        uploadTestFile("buildings_rosario.parquet");
        //        uploadTestFile("places_rosario.parquet");
        //        uploadTestFile("transportation_rosario.parquet");
    }

    private static void uploadTestFile(String filename) throws IOException {
        // Use the current instance since we can't instantiate the base class directly
        String resourcePath = TEST_DATA_DIR + "/" + filename;
        File source = new File(GeoParquetS3IT.class.getResource(resourcePath).getFile());
        File destination = new File(System.getProperty("java.io.tmpdir"), filename);
        FileUtils.copyFile(source, destination);

        s3Client.putObject(
                PutObjectRequest.builder().bucket(BUCKET_NAME).key(filename).build(), destination.toPath());
    }

    @Test
    public void testS3Access() throws Exception {
        // Get S3 URL for the test file
        String s3Url = String.format(
                "s3://%s/%s?endpoint=%s&region=%s&access_key=%s&secret_key=%s",
                BUCKET_NAME, "buildings_rosario.parquet", endpoint, "us-east-1", "test", "test");

        // Create datastore with S3 URL
        dataStore = createRemoteDataStore(s3Url);

        // Verify we can get the feature type name
        String[] typeNames = dataStore.getTypeNames();
        assertEquals("Should have one type name", 1, typeNames.length);
        assertEquals("Type name should match file name without extension", "buildings_rosario", typeNames[0]);

        // Get a feature source and verify it has features
        SimpleFeatureSource source = dataStore.getFeatureSource(typeNames[0]);
        verifyFeatureSource(source, -1);
    }

    @Test
    public void testS3SpatialFilter() throws Exception {
        // Get S3 URL for the test file
        String s3Url = String.format(
                "s3://%s/%s?endpoint=%s&region=%s&access_key=%s&secret_key=%s",
                BUCKET_NAME,
                "buildings_rosario.parquet",
                "http://" + localstack.getHost() + ":" + localstack.getFirstMappedPort(),
                "us-east-1",
                "test",
                "test");

        // Create datastore with S3 URL
        dataStore = createRemoteDataStore(s3Url);

        // Get the feature source
        SimpleFeatureSource source = dataStore.getFeatureSource("buildings_rosario");

        // Get total count
        int totalCount = source.getFeatures().size();

        // Create a spatial filter for central Rosario
        Geometry centralRosario = new WKTReader()
                .read("POLYGON((-60.65 -32.93, -60.61 -32.93, -60.61 -32.95, -60.65 -32.95, -60.65 -32.93))");

        Filter filter = FF.intersects(
                FF.property(source.getSchema().getGeometryDescriptor().getLocalName()), FF.literal(centralRosario));

        // Get filtered features
        int filteredCount = source.getFeatures(filter).size();

        // Should have fewer features than the total
        assertTrue("Filtered count should be less than total", filteredCount < totalCount);
        assertTrue("Should have some filtered features", filteredCount > 0);
    }

    @Test
    public void testS3AttributeFilter() throws Exception {
        // Get S3 URL for the test file
        String s3Url = String.format(
                "s3://%s/%s?endpoint=%s&region=%s&access_key=%s&secret_key=%s",
                BUCKET_NAME,
                "places_rosario.parquet",
                "http://" + localstack.getHost() + ":" + localstack.getFirstMappedPort(),
                "us-east-1",
                "test",
                "test");

        // Create datastore with S3 URL
        dataStore = createRemoteDataStore(s3Url);

        // Get the feature source
        SimpleFeatureSource source = dataStore.getFeatureSource("places_rosario");

        // Get total count
        int totalCount = source.getFeatures().size();

        // Create an attribute filter for a specific category
        Filter filter = FF.like(FF.property("class"), "food");

        // Get filtered features
        int filteredCount = source.getFeatures(filter).size();

        // Should have fewer features than the total
        assertTrue("Should have some filtered features", filteredCount > 0);
        assertTrue("Filtered count should be less than total", filteredCount < totalCount);
    }
}
