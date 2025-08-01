/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2025, Open Source Geospatial Foundation (OSGeo)
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
 *
 */
package org.geotools.pmtiles.store;

import io.tileverse.rangereader.RangeReader;
import io.tileverse.rangereader.RangeReaderFactory;
import io.tileverse.rangereader.spi.RangeReaderProvider;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFactorySpi;
import org.geotools.util.Converters;

public class PMTilesDataStoreFactory implements DataStoreFactorySpi {

    /** Optional - uri of the FeatureType's namespace */
    public static final Param NAMESPACEP = ParamBuilder.builder()
            .key("namespace")
            .title("Namespace prefix")
            .type(String.class)
            .optional()
            .advancedLevel()
            .build();

    /** URI to the PMTiles file. */
    public static final Param URIP = ParamBuilder.builder()
            .key("pmtiles")
            .type(String.class)
            .required(true)
            .title(
                    """
                    URI to a Protomaps PMTiles file containing Vector Tiles. Supports local files and cloud storage:
                    • Local files: file:/path/to/file.pmtiles
                    • AWS S3: s3://bucket/key.pmtiles or https://bucket.s3.amazonaws.com/key.pmtiles
                    • Azure Blob Storage: https://account.blob.core.windows.net/container/key.pmtiles
                    • Google Cloud Storage: https://storage.googleapis.com/bucket/key.pmtiles
                    • HTTP/HTTPS: https://example.com/path/file.pmtiles (with optional authentication)
                    • MinIO and S3-compatible services: http://localhost:9000/bucket/key.pmtiles
                    """)
            .metadata(Param.EXT, "pmtiles")
            .metadata(Param.IS_LARGE_TEXT, true)
            .build();

    public static final Param FORCE_RANGEREADER = RangeReaderParams.FORCE_RANGEREADER_PROVIDER_PARAM;

    static final PMTilesDataStoreFactory INSTANCE = new PMTilesDataStoreFactory();

    @Override
    public String getDisplayName() {
        return "PMTiles";
    }

    @Override
    public String getDescription() {
        return "Protomaps (.pmtiles) with vector tiles";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * Dynamically builds the list of configuration parameters based on which {@link RangeReaderProvider}s are avilable.
     *
     * @see RangeReaderParams#appendAfter(org.geotools.api.data.DataAccessFactory.Param...)
     */
    @Override
    public Param[] getParametersInfo() {
        return RangeReaderParams.appendAfter(NAMESPACEP, URIP, FORCE_RANGEREADER);
    }

    @Override
    public DataStore createNewDataStore(Map<String, ?> params) {
        throw new UnsupportedOperationException("Creating new PMTiles files is unsupported");
    }

    @Override
    public boolean canProcess(java.util.Map<String, ?> params) {
        boolean canProcess = DataStoreFactorySpi.super.canProcess(params);
        URI toURI;
        try {
            toURI = lookup(URIP, params, URI.class);
        } catch (Exception e) {
            return false;
        }
        if (canProcess && (toURI == null)) {
            return false;
        }
        if (!canProcess && toURI != null) {
            Map<String, Object> p = new HashMap<>(params);
            p.put(URIP.key, toURI.toString());
            return DataStoreFactorySpi.super.canProcess(p);
        }
        return canProcess;
    }

    @Override
    public PMTilesDataStore createDataStore(Map<String, ?> params) throws IOException {

        RangeReader reader = createRangeReader(params);
        PMTilesDataStore store = new PMTilesDataStore(reader);

        String namespaceURI = lookup(NAMESPACEP, params, String.class);
        store.setNamespaceURI(namespaceURI);

        return store;
    }

    static RangeReader createRangeReader(Map<String, ?> params) throws IOException {
        URI uri = lookup(URIP, params, URI.class);

        Properties rangeReaderConfig = RangeReaderParams.toProperties(params);
        return RangeReaderFactory.create(uri, rangeReaderConfig);
    }

    /**
     * Looks up a parameter, if not found it returns the default value, assuming there is one, or null otherwise
     *
     * @param <T>
     * @throws IOException
     */
    static <T> T lookup(Param param, Map<String, ?> params, Class<T> target) throws IOException {
        Object lookUp;
        if (param == URIP) {
            lookUp = params.get(URIP.key);

            if (lookUp instanceof URI uri) {
                lookUp = uri.toString();
            } else if (lookUp instanceof URL url) {
                lookUp = url.toExternalForm();
            } else if (lookUp instanceof Path path) {
                lookUp = path.toUri().toString();
            } else if (lookUp instanceof File file) {
                lookUp = file.toURI().toString();
            }
        } else {
            lookUp = param.lookUp(params);
        }
        if (lookUp == null) {
            lookUp = param.getDefaultValue();
        }
        return Converters.convert(lookUp, target);
    }
}
