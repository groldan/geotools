/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2003-2005, Open Geospatial Consortium Inc.
 *
 *    All Rights Reserved. http://www.opengis.org/legal/
 */
package org.geotools.api.referencing.operation;

import java.util.List;

/**
 * An ordered sequence of two or more single coordinate operations. The sequence of operations is constrained by the
 * requirement that the source coordinate reference system of step (<var>n</var>+1) must be the same as the target
 * coordinate reference system of step (<var>n</var>). The source coordinate reference system of the first step and the
 * target coordinate reference system of the last step are the source and target coordinate reference system associated
 * with the concatenated operation. Instead of a forward operation, an inverse operation may be used for one or more of
 * the operation steps mentioned above, if the inverse operation is uniquely defined by the forward operation.
 *
 * @version <A HREF="http://portal.opengeospatial.org/files/?artifact_id=6716">Abstract specification 2.0</A>
 * @author Martin Desruisseaux (IRD)
 * @since GeoAPI 1.0
 */
public interface ConcatenatedOperation extends CoordinateOperation {
    /**
     * Returns the sequence of operations.
     *
     * @return The sequence of operations.
     */
    List<SingleOperation> getOperations();
}
