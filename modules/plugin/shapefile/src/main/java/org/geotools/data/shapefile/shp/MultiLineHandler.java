/*
 * GeoTools - The Open Source Java GIS Toolkit http://geotools.org
 *
 * (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
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

import static org.geotools.data.shapefile.shp.ShapefileCoordinateSequence.copy;
import static org.geotools.data.shapefile.shp.ShapefileCoordinateSequence.readCoordinates;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

/*
 * $Id$ @author aaime @author Ian Schneider
 */
/**
 * The default JTS handler for shapefile. Currently uses the default JTS GeometryFactory, since it
 * doesn't seem to matter.
 */
public class MultiLineHandler implements ShapeHandler {
    final ShapeType shapeType;

    GeometryFactory geometryFactory;

    double[] xy;

    double[] z;

    private BooleanSupplier abortProcessing = () -> false;

    /** Create a MultiLineHandler for ShapeType.ARC */
    public MultiLineHandler(GeometryFactory gf) {
        shapeType = ShapeType.ARC;
        this.geometryFactory = gf;
    }

    /**
     * Create a MultiLineHandler for one of: <br>
     * ShapeType.ARC,ShapeType.ARCM,ShapeType.ARCZ
     *
     * @param type The ShapeType to use.
     * @throws ShapefileException If the ShapeType is not correct (see constructor).
     */
    public MultiLineHandler(ShapeType type, GeometryFactory gf) throws ShapefileException {
        if (!type.isLineType()) {
            throw new ShapefileException(
                    "MultiLineHandler constructor - expected type to be 3,13 or 23");
        }

        shapeType = type;
        this.geometryFactory = gf;
    }

    public @Override void setAbortSupplier(BooleanSupplier check) {
        Objects.requireNonNull(check);
        this.abortProcessing = check;
    }

    /** Get the type of shape stored (ShapeType.ARC,ShapeType.ARCM,ShapeType.ARCZ) */
    public ShapeType getShapeType() {
        return shapeType;
    }

    /** */
    public int getLength(Object geometry) {
        MultiLineString multi = (MultiLineString) geometry;

        int numlines;
        int numpoints;
        int length;

        numlines = multi.getNumGeometries();
        numpoints = multi.getNumPoints();

        if (shapeType == ShapeType.ARC) {
            length = 44 + (4 * numlines) + (numpoints * 16);
        } else if (shapeType == ShapeType.ARCM) {
            length = 44 + (4 * numlines) + (numpoints * 16) + 8 + 8 + (8 * numpoints);
        } else if (shapeType == ShapeType.ARCZ) {
            length =
                    44
                            + (4 * numlines)
                            + (numpoints * 16)
                            + 8
                            + 8
                            + (8 * numpoints)
                            + 8
                            + 8
                            + (8 * numpoints);
        } else {
            throw new IllegalStateException("Expected ShapeType of Arc, got " + shapeType);
        }

        return length;
    }

    private Object createNull() {
        return geometryFactory.createMultiLineString((LineString[]) null);
    }

    public Object read(ByteBuffer buffer, ShapeType type, boolean flatGeometry) {
        if (type == ShapeType.NULL) {
            return createNull();
        }

        // skip bounding box (not needed)
        buffer.position(buffer.position() + 4 * Double.BYTES);

        final int numParts = buffer.getInt();
        final int numPoints = buffer.getInt(); // total number of points
        final int[] partOffsets = new int[numParts];
        for (int i = 0; i < numParts; i++) {
            partOffsets[i] = buffer.getInt();
        }
        ShapefileCoordinateSequence coords = readCoordinates(buffer, numPoints, shapeType);
        if (flatGeometry) {
            coords = coords.force2D();
        }

        LineString[] lineStrings = new LineString[numParts];
        final CoordinateSequenceFactory csFac = geometryFactory.getCoordinateSequenceFactory();
        for (int part = 0; part < numParts; part++) {
            if (abortProcessing.getAsBoolean()) {
                return null;
            }
            int start = partOffsets[part];
            int finish = (part == numParts - 1) ? numPoints : partOffsets[part + 1];
            int length = finish - start;
            CoordinateSequence cs = coords.copy(start, finish, csFac);
            if (length == 1) {
                Coordinate c = cs.getCoordinate(0);
                cs = csFac.create(2, cs.getDimension(), cs.getMeasures());
                copy(cs, c, 0);
                copy(cs, c, 1);
            }
            lineStrings[part] = geometryFactory.createLineString(cs);
        }
        return geometryFactory.createMultiLineString(lineStrings);
    }

    public void write(ByteBuffer buffer, Object geometry) {
        MultiLineString multi = (MultiLineString) geometry;

        Envelope box = multi.getEnvelopeInternal();
        buffer.putDouble(box.getMinX());
        buffer.putDouble(box.getMinY());
        buffer.putDouble(box.getMaxX());
        buffer.putDouble(box.getMaxY());

        final int numParts = multi.getNumGeometries();
        final CoordinateSequence[] lines = new CoordinateSequence[numParts];
        final double[] zExtreame = {Double.NaN, Double.NaN};
        final int npoints = multi.getNumPoints();

        buffer.putInt(numParts);
        buffer.putInt(npoints);

        {
            int idx = 0;
            for (int i = 0; i < numParts; i++) {
                lines[i] = ((LineString) multi.getGeometryN(i)).getCoordinateSequence();
                buffer.putInt(idx);
                idx = idx + lines[i].size();
            }
        }

        for (int lineN = 0; lineN < lines.length; lineN++) {
            CoordinateSequence coords = lines[lineN];
            if (shapeType == ShapeType.ARCZ) {
                JTSUtilities.zMinMax(coords, zExtreame);
            }
            final int ncoords = coords.size();

            for (int t = 0; t < ncoords; t++) {
                buffer.putDouble(coords.getX(t));
                buffer.putDouble(coords.getY(t));
            }
        }

        if (shapeType == ShapeType.ARCZ) {
            if (Double.isNaN(zExtreame[0])) {
                buffer.putDouble(0.0);
                buffer.putDouble(0.0);
            } else {
                buffer.putDouble(zExtreame[0]);
                buffer.putDouble(zExtreame[1]);
            }

            for (int lineN = 0; lineN < lines.length; lineN++) {
                final CoordinateSequence coords = lines[lineN];
                final int ncoords = coords.size();
                double z;
                for (int t = 0; t < ncoords; t++) {
                    z = coords.getOrdinate(t, 2);
                    if (Double.isNaN(z)) {
                        buffer.putDouble(0.0);
                    } else {
                        buffer.putDouble(z);
                    }
                }
            }
        }

        // if there are M coordinates
        if (shapeType == ShapeType.ARCZ || shapeType == ShapeType.ARCM) {
            // get M values list
            List<Double> mvalues = new ArrayList<>();
            for (int t = 0, tt = multi.getNumGeometries(); t < tt; t++) {
                LineString line = (LineString) multi.getGeometryN(t);
                CoordinateSequence seq = line.getCoordinateSequence();
                for (int i = 0; i < seq.size(); i++) {
                    mvalues.add(line.getCoordinateSequence().getM(i));
                }
            }

            // min, max
            buffer.putDouble(mvalues.stream().min(Double::compare).get());
            buffer.putDouble(mvalues.stream().max(Double::compare).get());
            // encode all M values
            mvalues.forEach(
                    x -> {
                        buffer.putDouble(x);
                    });
        }
    }
}

/*
 * $Log: MultiLineHandler.java,v $ Revision 1.4 2003/11/13 22:10:35 jmacgill cast a null to avoid
 * ambigous call with JTS1.4
 *
 * Revision 1.3 2003/07/23 23:41:09 ianschneider more testing updates
 *
 * Revision 1.2 2003/07/23 00:59:59 ianschneider Lots of PMD fix ups
 *
 * Revision 1.1 2003/05/14 17:51:20 ianschneider migrated packages
 *
 * Revision 1.3 2003/04/30 23:19:45 ianschneider Added construction of multi geometries for default
 * return values, even if only one geometry. This could have effects through system.
 *
 * Revision 1.2 2003/03/30 20:21:09 ianschneider Moved buffer branch to main
 *
 * Revision 1.1.2.3 2003/03/12 15:30:14 ianschneider made ShapeType final for handlers - once
 * they're created, it won't change.
 *
 * Revision 1.1.2.2 2003/03/07 00:36:41 ianschneider
 *
 * Added back the additional ShapeType parameter in ShapeHandler.read. ShapeHandler's need return
 * their own special "null" shape if needed. Fixed the ShapefileReader to not throw exceptions for
 * "null" shapes. Fixed ShapefileReader to accomodate junk after the last valid record. The theory
 * goes, if the shape number is proper, that is, one greater than the previous, we consider that a
 * valid record and attempt to read it. I suppose, by chance, the junk could coincide with the next
 * record number. Stupid ESRI. Fixed some record-length calculations which resulted in writing of
 * bad shapefiles.
 *
 * Revision 1.1.2.1 2003/03/06 01:16:34 ianschneider
 *
 * The initial changes for moving to java.nio. Added some documentation and improved exception
 * handling. Works for reading, may work for writing as of now.
 *
 * Revision 1.1 2003/02/27 22:35:50 aaime New shapefile module, initial commit
 *
 * Revision 1.2 2003/01/22 18:31:05 jaquino Enh: Make About Box configurable
 *
 * Revision 1.3 2002/10/30 22:36:11 dblasby Line reader now returns LINESTRING(..) if there is only
 * one part to the arc polyline.
 *
 * Revision 1.2 2002/09/09 20:46:22 dblasby Removed LEDatastream refs and replaced with
 * EndianData[in/out]putstream
 *
 * Revision 1.1 2002/08/27 21:04:58 dblasby orginal
 *
 * Revision 1.2 2002/03/05 10:23:59 jmacgill made sure geometries were created using the factory
 * methods
 *
 * Revision 1.1 2002/02/28 00:38:50 jmacgill Renamed files to more intuitve names
 *
 * Revision 1.3 2002/02/13 00:23:53 jmacgill First semi working JTS version of Shapefile code
 *
 * Revision 1.2 2002/02/11 18:42:45 jmacgill changed read and write statements so that they produce
 * and take Geometry objects instead of specific MultiLine objects changed parts[] array name to
 * partOffsets[] for clarity and consistency with ShapePolygon
 *
 * Revision 1.1 2002/02/11 16:54:43 jmacgill added shapefile code and directories
 *
 */
