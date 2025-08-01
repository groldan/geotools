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
package org.geotools.vectortiles.store;

import static java.util.Objects.requireNonNull;

import io.tileverse.jackson.databind.tilejson.v3.VectorLayer;
import io.tileverse.pmtiles.store.VectorTileStore;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.geotools.api.data.DataStoreFactorySpi;
import org.geotools.api.feature.type.Name;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;

public abstract class VectorTilesDataStore extends ContentDataStore {

    static final Logger LOGGER = Logging.getLogger(VectorTilesDataStore.class);

    public static final GeometryFactory DEFAULT_GEOMETRY_FACTORY =
            new GeometryFactory(new PackedCoordinateSequenceFactory());

    private VectorTileStore tileStore;

    protected VectorTilesDataStore(DataStoreFactorySpi factory, VectorTileStore tileStore) throws IOException {
        this.tileStore = requireNonNull(tileStore, "tileStore is null");
        setDataStoreFactory(requireNonNull(factory, "factory is null"));
        setFeatureTypeFactory(CommonFactoryFinder.getFeatureTypeFactory(null));
        setFeatureFactory(CommonFactoryFinder.getFeatureFactory(null));
        setFilterFactory(CommonFactoryFinder.getFilterFactory());
        setGeometryFactory(VectorTilesDataStore.DEFAULT_GEOMETRY_FACTORY);
    }

    public VectorTileStore getTileStore() {
        return this.tileStore;
    }

    @Override
    protected List<Name> createTypeNames() {
        return tileStore.getVectorLayersMetadata().stream()
                .map(VectorLayer::id)
                .map(super::name)
                .toList();
    }

    @Override
    protected VectorTilesFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
        String typeName = entry.getTypeName();
        Optional<VectorLayer> layerMetadata = tileStore.getLayerMetadata(typeName);
        if (layerMetadata.isEmpty()) {
            throw new IOException("Vector layer %s does not exist.".formatted(typeName));
        }
        return new VectorTilesFeatureSource(entry, layerMetadata.orElseThrow());
    }
}
