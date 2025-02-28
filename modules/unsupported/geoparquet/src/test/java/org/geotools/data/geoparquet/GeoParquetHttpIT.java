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

import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.filter.Filter;
import org.geotools.filter.FilterFactoryImpl;
import org.junit.ClassRule;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/** Tests for GeoParquet DataStore with HTTP access. */
public class GeoParquetHttpIT extends GeoParquetTestBase {

    private static final FilterFactoryImpl FF = new FilterFactoryImpl();

    // We use a simple HTTP server container (nginx)
    @SuppressWarnings("resource")
    @ClassRule
    public static GenericContainer<?> httpServer = new GenericContainer<>(DockerImageName.parse("nginx:1.21-alpine"))
            .withExposedPorts(80)
            .withFileSystemBind(
                    "src/test/resources/test-data/overturemaps", "/usr/share/nginx/html", BindMode.READ_ONLY);

    @Test
    public void testHttpAccess() throws Exception {
        // Get the HTTP URL to the test file
        String httpUrl = String.format(
                "http://%s:%d/buildings_rosario.parquet", httpServer.getHost(), httpServer.getFirstMappedPort());

        // Create the datastore with the HTTP URL
        dataStore = createRemoteDataStore(httpUrl);

        // Verify we can get the feature type name
        String[] typeNames = dataStore.getTypeNames();
        assertEquals("Should have one type name", 1, typeNames.length);
        assertEquals("Type name should match file name without extension", "buildings_rosario", typeNames[0]);

        // Get a feature source and verify it has features
        SimpleFeatureSource source = dataStore.getFeatureSource(typeNames[0]);
        verifyFeatureSource(source, -1);
    }

    @Test
    public void testHttpSpatialFilter() throws Exception {
        // Get the HTTP URL to the test file
        String httpUrl = String.format(
                "http://%s:%d/buildings_rosario.parquet", httpServer.getHost(), httpServer.getFirstMappedPort());

        // Create the datastore with the HTTP URL
        dataStore = createRemoteDataStore(httpUrl);

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
    public void testHttpAttributeFilter() throws Exception {
        // Get the HTTP URL to the test file
        String httpUrl = String.format(
                "http://%s:%d/places_rosario.parquet", httpServer.getHost(), httpServer.getFirstMappedPort());

        // Create the datastore with the HTTP URL
        dataStore = createRemoteDataStore(httpUrl);

        // Get the feature source
        SimpleFeatureSource source = dataStore.getFeatureSource("places_rosario");

        // Get total count
        int totalCount = source.getFeatures().size();

        // Create an attribute filter for a specific category
        // Note: adjust the property name and value to match your actual data
        Filter filter = FF.like(FF.property("class"), "food");

        // Get filtered features
        int filteredCount = source.getFeatures(filter).size();

        // Should have fewer features than the total
        assertTrue("Should have some filtered features", filteredCount > 0);
        assertTrue("Filtered count should be less than total", filteredCount < totalCount);
    }
}
