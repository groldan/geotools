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

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.CoordinateXYM;
import org.locationtech.jts.geom.CoordinateXYZM;
import org.locationtech.jts.geom.Envelope;

/**
 * {@link DoubleBuffer} backed polygon coordinates list in shapefile format: all {@code [x,y]},
 * followed by all {@code [z]}, followed by all {@code [m]}
 */
final class ShapefileCoordinateSequence implements CoordinateSequence {

    private final DoubleBuffer buffer;
    private final boolean hasZ;
    private final boolean hasM;
    private final int size;
    private final int zOffset;
    private final int mOffset;
    private final int dimensions;

    ShapefileCoordinateSequence(
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
        this.mOffset = hasM ? boundingRangeSize + (hasZ ? zOffset + size : 2 * size) : -1;
        this.dimensions = 2 + (hasZ ? 1 : 0) + (hasM ? 1 : 0);
    }

    private ShapefileCoordinateSequence(ShapefileCoordinateSequence orig, boolean force2D) {
        this.buffer = orig.buffer;
        this.size = orig.size;
        this.hasZ = force2D ? false : orig.hasZ;
        this.zOffset = force2D ? -1 : orig.zOffset;

        this.hasM = force2D ? false : orig.hasM;
        this.mOffset = force2D ? -1 : orig.mOffset;
        this.dimensions = force2D ? 2 : orig.dimensions;
    }

    public static ShapefileCoordinateSequence readCoordinates(
            final ByteBuffer buffer, final int numPoints, final ShapeType shapeType) {
        boolean hasZ = false;
        boolean hasM = false;
        switch (shapeType) {
            case POINT:
            case MULTIPOINT:
            case ARC:
            case POLYGON:
                // ignore, no Z nor M
                break;
            case ARCZ:
            case MULTIPOINTZ:
            case POINTZ:
            case POLYGONZ:
                hasZ = true;
                hasM = true; // hasZ implies hasM as per the shapefile spec
                break;
            case POINTM:
            case MULTIPOINTM:
            case ARCM:
            case POLYGONM:
                hasM = true;
                break;
            default:
                throw new IllegalArgumentException(
                        "ShapeType must be a concrete type: " + shapeType);
        }
        DoubleBuffer dbuffer = buffer.asDoubleBuffer();
        ShapefileCoordinateSequence seq =
                new ShapefileCoordinateSequence(dbuffer, numPoints, hasZ, hasM);
        return seq;
    }

    public ShapefileCoordinateSequence force2D() {
        if (this.hasZ) {
            return new ShapefileCoordinateSequence(this, true);
        }
        return this; // no Z, just return this
    }

    public CoordinateSequence copy(
            int fromInclussive, int toExclussive, CoordinateSequenceFactory factory) {
        if (fromInclussive < 0) throw new IllegalArgumentException("from < 0");
        if (toExclussive <= fromInclussive) throw new IllegalArgumentException("to <= from");

        int size = toExclussive - fromInclussive;
        CoordinateSequence target = factory.create(size, this.dimensions, this.hasM ? 1 : 0);
        copy(target, fromInclussive, toExclussive);
        return target;
    }

    public CoordinateSequence copyAutoClosing(
            int fromInclussive, int toExclussive, CoordinateSequenceFactory factory) {
        Coordinate c1 = getCoordinate(fromInclussive);
        Coordinate c2 = getCoordinate(toExclussive - 1);
        if (c1.equals3D(c2)) {
            return copy(fromInclussive, toExclussive, factory);
        }
        int length = 1 + toExclussive - fromInclussive;
        CoordinateSequence target = factory.create(length, this.dimensions, this.hasM ? 1 : 0);
        copy(target, fromInclussive, toExclussive);
        copy(target, c1, length - 1);
        return target;
    }

    private void copy(CoordinateSequence target, int from, int to) {
        Coordinate c = newCoordinate();
        for (int i = from, t = 0; i < to; i++, t++) {
            this.getCoordinate(i, c);
            copy(target, c, t);
        }
    }

    public static void copy(CoordinateSequence target, Coordinate source, int targetIndex) {
        target.setOrdinate(targetIndex, 0, source.getX());
        target.setOrdinate(targetIndex, 1, source.getY());
        if (target.hasZ()) target.setOrdinate(targetIndex, 2, source.getZ());
        if (target.hasM()) target.setOrdinate(targetIndex, target.hasZ() ? 3 : 2, source.getM());
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

    private Coordinate newCoordinate() {
        if (hasM && hasZ) {
            return new CoordinateXYZM();
        } else if (hasM) {
            return new CoordinateXYM();
        } else if (hasZ) {
            return new Coordinate();
        }
        return new CoordinateXY();
    }

    public @Override Coordinate getCoordinate(int i) {
        Coordinate c = newCoordinate();
        getCoordinate(i, c);
        return c;
    }

    public @Override Coordinate getCoordinateCopy(int i) {
        throw new UnsupportedOperationException();
    }

    public @Override void getCoordinate(int index, Coordinate coord) {
        coord.setX(getOrdinate(index, 0));
        coord.setY(getOrdinate(index, 1));
        if (hasZ) {
            coord.setZ(getOrdinate(index, 2));
        }
        if (hasM) {
            coord.setM(getOrdinate(index, hasZ ? 3 : 2));
        }
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
                offset = (hasZ ? (index + zOffset) : (hasM ? (index + mOffset) : -1));
                break;
            case 3:
                offset = hasM ? (index + mOffset) : -1;
                break;
            default:
                throw new ArrayIndexOutOfBoundsException(ordinateIndex);
        }
        double value = offset == -1 ? Double.NaN : buffer.get(offset);
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
