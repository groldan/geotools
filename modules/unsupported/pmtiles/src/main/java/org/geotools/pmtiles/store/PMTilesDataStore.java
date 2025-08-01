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
 */
package org.geotools.pmtiles.store;

import com.google.common.annotations.VisibleForTesting;
import io.tileverse.pmtiles.PMTilesReader;
import io.tileverse.pmtiles.store.PMTilesVectorTileStore;
import io.tileverse.pmtiles.store.VectorTileStore;
import io.tileverse.rangereader.RangeReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.geotools.vectortiles.store.VectorTilesDataStore;

public class PMTilesDataStore extends VectorTilesDataStore {

    static final Logger LOGGER = Logging.getLogger(PMTilesDataStore.class);

    private RangeReader rangeReader;

    public PMTilesDataStore(RangeReader rangeReader) throws IOException {
        super(PMTilesDataStoreFactory.INSTANCE, createTileStore(rangeReader));
        this.rangeReader = rangeReader;
    }

    @VisibleForTesting
    PMTilesDataStore(PMTilesReader pmtiles) throws IOException {
        super(PMTilesDataStoreFactory.INSTANCE, new PMTilesVectorTileStore(pmtiles));
    }

    private static VectorTileStore createTileStore(RangeReader rangeReader) throws IOException {
        PMTilesReader pmtilesReader = new PMTilesReader(rangeReader::asByteChannel);
        return new PMTilesVectorTileStore(pmtilesReader);
    }

    @Override
    public void dispose() {
        try {
            super.dispose();
        } finally {
            closeRangeReader();
        }
    }

    private void closeRangeReader() {
        RangeReader reader = this.rangeReader;
        this.rangeReader = null;
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
