/*
 * GeoTools - The Open Source Java GIS Toolkit http://geotools.org
 *
 * (C) 2002-2009, Open Source Geospatial Foundation (OSGeo)
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; version 2.1 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.geotools.data.postgis;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.jdbc.JDBCFeatureReaderOnlineTest;
import org.geotools.jdbc.JDBCTestSetup;
import org.geotools.jdbc.SQLDialect;
import org.geotools.util.factory.Hints;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class PostgisFeatureReaderOnlineTest extends JDBCFeatureReaderOnlineTest {

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new PostGISTestSetup();
    }

    public void testQueryTimeout() throws Exception {
        final SQLDialect dialect = spy(super.dialect);
        super.dataStore.setSQLDialect(dialect);
        final Query query = new Query(tname("ft1"));

        // make the dialect add a pg_sleep(3) to the column names for the query to take at least 3
        // seconds
        doAnswer(
                        invocation -> {
                            StringBuffer sql = invocation.getArgument(1);
                            invocation.callRealMethod();
                            sql.append(", pg_sleep(3) as slept");
                            return null;
                        })
                .when(dialect)
                .encodePostSelect(any(), any());

        // do a pre-flight run to demonstrate pg_sleep works
        long start = System.currentTimeMillis();
        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                dataStore.getFeatureReader(query, Transaction.AUTO_COMMIT)) {
            long end = System.currentTimeMillis();
            double timeSecs = (end - start) / 1000D;
            assertTrue("Query should have taken at least 3 seconds: " + timeSecs, timeSecs >= 3);
            int count = 0;
            for (; reader.hasNext(); reader.next(), count++) ;
            assertEquals(3, count);
        }

        // now set a query timeout of 1 second
        final int timeoutSeconds = 1;
        query.getHints().put(Hints.QUERY_TIMEOUT_SECONDS, timeoutSeconds);
        start = System.currentTimeMillis();
        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                dataStore.getFeatureReader(query, Transaction.AUTO_COMMIT)) {

        } catch (IOException expected) {
            expected.printStackTrace();
            long end = System.currentTimeMillis();
            double timeSecs = (end - start) / 1000D;
            assertTrue(
                    "Query should have failead at around 1 second: " + timeSecs, timeSecs >= 1.0);
            assertTrue("Query should have failed before 2 seconds: " + timeSecs, timeSecs < 2.0);
        }
    }
}
