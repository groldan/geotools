/*
 * GeoTools - The Open Source Java GIS Toolkit http://geotools.org
 *
 * (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; version 2.1 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.geotools.data.shapefile.shp;

import java.nio.DoubleBuffer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;

/**
 * {@link DoubleBuffer} backed polygon coordinates list in shapefile format: all {@code [x,y]},
 * followed by all {@code [z]}, followed by all {@code [m]}
 */
class BufferCoordinateSequence implements CoordinateSequence {

    private final DoubleBuffer buffer;
    private final boolean hasZ;
    private final boolean hasM;
    private final int size;
    private final int zOffset;
    private final int mOffset;
    private final int dimensions;

    BufferCoordinateSequence(
            DoubleBuffer buffer, final int size, final boolean hasZ, final boolean hasM) {
        if (hasM && !hasZ) {
            throw new IllegalArgumentException("hasM implies hasZ");
        }
        final int boundingRangeSize = 2;
        this.buffer = buffer;
        this.size = size;
        this.hasZ = hasZ;
        this.hasM = hasM;
        this.zOffset = hasZ ? boundingRangeSize + 2 * size : -1;
        this.mOffset = !hasM ? -1 : boundingRangeSize + (hasZ ? zOffset + size : 2 * size);
        this.dimensions = 2 + (hasZ ? 1 : 0) + (hasM ? 1 : 0);
    }

    public @Override int size() {
        return size;
    }

    public @Override int getDimension() {
        return this.dimensions;
    }

    public @Override int getMeasures() {
        return hasM ? 1 : 0;
    }

    public @Override Coordinate getCoordinate(int i) {
        throw new UnsupportedOperationException();
    }

    public @Override Coordinate getCoordinateCopy(int i) {
        throw new UnsupportedOperationException();
    }

    public @Override void getCoordinate(int index, Coordinate coord) {
        throw new UnsupportedOperationException();
    }

    public @Override double getX(int index) {
        return getOrdinate(index, CoordinateSequence.X);
    }

    public @Override double getY(int index) {
        return getOrdinate(index, CoordinateSequence.Y);
    }

    public @Override double getOrdinate(final int index, final int ordinateIndex) {
        int offset;
        switch (ordinateIndex) {
            case 0:
                offset = 2 * index;
                break;
            case 1:
                offset = 1 + 2 * index;
                break;
            case 2:
                offset = (hasZ ? zOffset : mOffset) + index;
                break;
            case 3:
                offset = mOffset + index;
                break;
            default:
                throw new ArrayIndexOutOfBoundsException(ordinateIndex);
        }
        double value = buffer.get(offset);
        return value;
    }

    public @Override void setOrdinate(int index, int ordinateIndex, double value) {
        throw new UnsupportedOperationException("read only coordinate sequence");
    }

    public @Override Coordinate[] toCoordinateArray() {
        throw new UnsupportedOperationException();
    }

    public @Override Envelope expandEnvelope(Envelope env) {
        throw new UnsupportedOperationException();
    }

    public @Override CoordinateSequence copy() {
        throw new UnsupportedOperationException();
    }

    public @Override CoordinateSequence clone() {
        throw new UnsupportedOperationException();
    }
}
