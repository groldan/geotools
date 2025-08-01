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

import com.google.api.client.util.store.DataStoreFactory;
import io.tileverse.rangereader.RangeReaderFactory;
import io.tileverse.rangereader.spi.RangeReaderConfig;
import io.tileverse.rangereader.spi.RangeReaderParameter;
import io.tileverse.rangereader.spi.RangeReaderProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.geotools.api.data.DataAccessFactory.Param;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;

public class RangeReaderParams {

    private static final Logger logger = Logging.getLogger(RangeReaderParams.class);

    /**
     * Param {@link DataStoreFactory} can use to force selecting a specific {@link RangeReaderProvider} with
     * {@link RangeReaderParams#toProperties(Map) toProperties(connectionParameters)} and used to obtain the range reder
     * through {@link RangeReaderFactory#create(Properties)} or {@link RangeReaderConfig#fromProperties(Properties)}
     */
    public static Param FORCE_RANGEREADER_PROVIDER_PARAM = toDataStoreParam(RangeReaderConfig.FORCE_PROVIDER_ID);

    /**
     * Aggregated list of supported {@link RangeReaderProvider#getParameters() range reader parameters} converted to
     * {@link Param DataAccessFactory.Param}
     */
    private static List<Param> PROVIDER_PARAMS = List.of();

    public static Param[] appendAfter(Param... dataStoreParams) {
        List<Param> rangeReaderParams = rangeReaderParams();
        return Stream.concat(Stream.of(dataStoreParams), rangeReaderParams.stream())
                .toArray(Param[]::new);
    }

    static List<Param> rangeReaderParams() {
        if (PROVIDER_PARAMS.isEmpty()) {
            List<RangeReaderProvider> availableRangeReaders = RangeReaderProvider.getAvailableProviders();

            // preserve order but make them unique, providers share the caching config parameters
            LinkedHashMap<String, Param> uniqueParams = new LinkedHashMap<>();
            for (RangeReaderProvider provider : availableRangeReaders) {
                List<RangeReaderParameter<?>> parameters = provider.getParameters();
                for (RangeReaderParameter<?> rrp : parameters) {
                    if (!uniqueParams.containsKey(rrp.key())) {
                        Param dsParam = toDataStoreParam(rrp);
                        uniqueParams.put(dsParam.key, dsParam);
                    }
                }
            }
            PROVIDER_PARAMS = uniqueParams.values().stream().toList();
        }
        return PROVIDER_PARAMS;
    }

    static Param toDataStoreParam(RangeReaderParameter<?> param) {
        logger.fine("Creating DataStoreFactory param for " + param);

        Object defaultValue = param.defaultValue().orElse(null);
        Object[] options;
        if (param.sampleValues().isEmpty()) {
            options = null;
        } else {
            options = param.sampleValues().toArray();
            // ugh, ParamInfo will set the value to the first option
            if (defaultValue == null && param.type().equals(String.class)) {
                defaultValue = "";
                options = Stream.concat(Stream.of(defaultValue), Stream.of(options))
                        .toArray();
            }
        }

        return ParamBuilder.builder()
                .key(param.key())
                // meh, ParamInfo uses title for tooltips, we want the description
                // .title(param.title())
                .title(param.description())
                .description(param.description())
                .optional()
                .level(param.group())
                .type(param.type())
                .defaultValue(defaultValue)
                .options(options)
                .build();
    }

    /**
     * Return a {@link Properties} object that can be used as an argument for {@link RangeReaderFactory}
     *
     * @param connectionParams
     * @return
     */
    public static Properties toProperties(Map<String, ?> connectionParams) {
        Properties configOpts = new Properties();
        addProperty(FORCE_RANGEREADER_PROVIDER_PARAM, connectionParams, configOpts);
        rangeReaderParams().forEach(param -> addProperty(param, connectionParams, configOpts));
        return configOpts;
    }

    private static void addProperty(Param param, Map<String, ?> params, Properties configOpts) {
        Object lookUp;
        try {
            lookUp = param.lookUp(params);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (lookUp != null) {
            String val = Converters.convert(lookUp, String.class);
            configOpts.setProperty(param.key, val);
        }
    }
}
