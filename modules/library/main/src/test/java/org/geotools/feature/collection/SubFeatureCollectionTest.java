/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2005-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.feature.collection;

import static org.junit.Assert.assertNotNull;

import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterVisitor;
import org.geotools.data.DataTestCase;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Test;

public class SubFeatureCollectionTest extends DataTestCase {
    DefaultFeatureCollection features = new DefaultFeatureCollection(null, null);

    @Override
    public void init() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("Dummy");

        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());

        for (int i = 0; i < 100; i++) {
            features.add(b.buildFeature(null));
        }
    }

    @Test
    public void testBounds() {
        SimpleFeatureCollection subCollection = features.subCollection(new Filter() {

            @Override
            public Object accept(FilterVisitor arg0, Object arg1) {
                return null;
            }

            @Override
            public boolean evaluate(Object arg0) {
                return true;
            }
        });

        // Should not throw an UnsupportedOperationException
        // TODO Not semantically testing the bounds
        assertNotNull(subCollection.getBounds());
    }
}
