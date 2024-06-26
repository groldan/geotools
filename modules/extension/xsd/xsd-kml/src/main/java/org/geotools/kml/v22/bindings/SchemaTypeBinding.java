/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2019, Open Source Geospatial Foundation (OSGeo)
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

package org.geotools.kml.v22.bindings;

import javax.xml.namespace.QName;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.Schema;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.Geometries;
import org.geotools.kml.v22.KML;
import org.geotools.kml.v22.SchemaRegistry;
import org.geotools.xs.XS;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/kml/2.2:SchemaType.
 *
 * <p>
 *
 * <pre>
 *  <code>
 *  &lt;complexType final="#all" name="SchemaType"&gt;
 *      &lt;sequence&gt;
 *          &lt;element maxOccurs="unbounded" minOccurs="0" ref="kml:SimpleField"/&gt;
 *          &lt;element maxOccurs="unbounded" minOccurs="0" ref="kml:SchemaExtension"/&gt;
 *      &lt;/sequence&gt;
 *      &lt;attribute name="name" type="string"/&gt;
 *      &lt;attribute name="id" type="ID"/&gt;
 *  &lt;/complexType&gt;
 *
 *   </code>
 * </pre>
 *
 * @generated
 */
public class SchemaTypeBinding extends AbstractComplexBinding {

    private SchemaRegistry schemaRegistry;

    public SchemaTypeBinding(SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    /** @generated */
    @Override
    public QName getTarget() {
        return KML.SchemaType;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    @Override
    public Class getType() {
        return SimpleFeatureType.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {

        String featureTypeName = null;
        String featureTypeId = null;

        if (node.hasAttribute("id")) {
            featureTypeId = (String) node.getAttributeValue("id");
        }

        if (node.hasAttribute("name")) {
            featureTypeName = (String) node.getAttributeValue("name");
        } else if (featureTypeId != null) {
            featureTypeName = featureTypeId;
        } else {
            featureTypeName = "feature";
        }

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName(featureTypeName);
        // TODO: crs

        for (Node n : node.getChildren("SimpleField")) {
            String name = (String) n.getAttributeValue("name");
            String typeName = (String) n.getAttributeValue("type");
            if (name != null && typeName != null) {
                tb.add(name, mapTypeName(typeName));
            }
        }
        SimpleFeatureType featureType = tb.buildFeatureType();
        schemaRegistry.add(featureTypeName, featureType);
        if (featureTypeId != null) {
            schemaRegistry.add(featureTypeId, featureType);
        }
        return featureType;
    }

    private Class mapTypeName(String typeName) {
        // try xs simple type
        Schema xsTypeMappingProfile = XS.getInstance().getTypeMappingProfile();
        NameImpl name = new NameImpl(XS.NAMESPACE, typeName);
        if (xsTypeMappingProfile.containsKey(name)) {
            AttributeType type = xsTypeMappingProfile.get(name);
            if (type.getBinding() != null) {
                return type.getBinding();
            }
        }

        // try gml geometry types
        Geometries g = Geometries.getForName(typeName);
        if (g != null) {
            return g.getBinding();
        }

        // default
        return String.class;
    }
}
