package ch.geowerkstatt.xtfdifftool.value;

import ch.ehi.basics.types.OutParam;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.interlis.ili2c.metamodel.CoordType;
import ch.interlis.ili2c.metamodel.MultiCoordType;
import ch.interlis.ili2c.metamodel.MultiPolylineType;
import ch.interlis.ili2c.metamodel.MultiSurfaceOrAreaType;
import ch.interlis.ili2c.metamodel.PolylineType;
import ch.interlis.ili2c.metamodel.SurfaceOrAreaType;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.itf.impl.jtsext.geom.JtsextGeometryFactory;
import ch.interlis.iom_j.itf.impl.jtsext.io.WKTWriterJtsext;
import ch.interlis.iox.IoxException;
import ch.interlis.iox_j.jts.Iox2jts;
import ch.interlis.iox_j.jts.Iox2jtsException;
import ch.interlis.iox_j.jts.Iox2jtsext;
import ch.interlis.iox_j.logging.LogEventFactory;
import com.fasterxml.jackson.annotation.JsonValue;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTWriter;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.stream.Stream;

public final class GeometryValue implements Value {
    private static final double LOCATION_ARC_TOLERANCE = 0.01;
    private static final PrecisionModel LOCATION_PRECISION = new PrecisionModel(1000);
    private static final GeometryFactory LOCATION_FACTORY = new GeometryFactory(LOCATION_PRECISION);

    @JsonValue
    private final String wktValue;
    private final String location;

    GeometryValue(String wktValue, String location) {
        this.wktValue = wktValue;
        this.location = location;
    }

    /**
     * Create a {@link GeometryValue} from an {@link IomObject} attribute.
     * @see ValueFactory.CreateValue#createValue(IomObject, String, Integer, Type, ValueFactory) ValueFactory.CreateValue
     */
    public static Optional<GeometryValue> createValue(IomObject obj, String attributeName, Integer index, Type type, ValueFactory factory) {
        var geometryObject = obj.getattrobj(attributeName, index == null ? 0 : index);

        try {
            var conversion = convert(geometryObject, type);
            if (conversion == null) {
                return Optional.empty();
            }

            var wktValue = new WKTWriterJtsext(3).write(conversion.value());
            var location = factory.includesLocations() ? writeLocation(conversion.location().convert()) : null;
            return Optional.of(new GeometryValue(wktValue, location));
        } catch (IoxException | Iox2jtsException e) {
            throw new RuntimeException(e);
        }
    }

    // Value and location side by side per type, so the two cannot drift apart. The value stays exact, the location
    // approximates arcs within the tolerance. A point has no arc, so its value is its location.
    private static Conversion convert(IomObject geometryObject, Type type) throws IoxException, Iox2jtsException {
        return switch (type.resolveAliases()) {
            case MultiCoordType _ -> Conversion.exact(Iox2jts.multicoord2JTS(geometryObject));
            case CoordType _ -> Conversion.exact((new JtsextGeometryFactory()).createPoint(Iox2jtsext.coord2JTS(geometryObject)));
            case PolylineType _ -> new Conversion(
                    Iox2jtsext.polyline2JTS(geometryObject, false, 0),
                    () -> Iox2jts.polyline2JTSlineString(geometryObject, false, LOCATION_ARC_TOLERANCE));
            case MultiPolylineType _ -> new Conversion(
                    Iox2jts.multipolyline2JTS(geometryObject, 0),
                    () -> Iox2jts.multipolyline2JTS(geometryObject, LOCATION_ARC_TOLERANCE));
            case SurfaceOrAreaType _ -> new Conversion(
                    Iox2jtsext.surface2JTS(geometryObject, 0),
                    () -> Iox2jts.surface2JTS(geometryObject, LOCATION_ARC_TOLERANCE));
            case MultiSurfaceOrAreaType _ -> new Conversion(
                    Iox2jtsext.multisurface2JTS(geometryObject, 0, new OutParam<>(), new LogEventFactory(), 0.0, "warning"),
                    () -> Iox2jts.multisurface2JTS(geometryObject, LOCATION_ARC_TOLERANCE, 0));
            case null, default -> null;
        };
    }

    // The location as a simple feature every map reads: in 2D, rounded to mm.
    private static String writeLocation(Geometry geometry) {
        var rounded = LOCATION_FACTORY.createGeometry(geometry);
        rounded.apply((CoordinateFilter) LOCATION_PRECISION::makePrecise);
        rounded.geometryChanged();
        return new WKTWriter().write(rounded);
    }

    /**
     * The exact geometry of a value and how to get its location, which only runs when the locations are wanted.
     *
     * @param value    the exact geometry, written as the value
     * @param location the conversion for the location
     */
    private record Conversion(Geometry value, LocationConversion location) {
        static Conversion exact(Geometry geometry) {
            return new Conversion(geometry, () -> geometry);
        }
    }

    /**
     * Converts a geometry for its location, with the checked exceptions of the iox-ili conversions.
     */
    @FunctionalInterface
    private interface LocationConversion {
        Geometry convert() throws IoxException, Iox2jtsException;
    }

    @Override
    public Stream<String> getLocations() {
        return Stream.ofNullable(location);
    }

    @Override
    public int compareTo(@NonNull Value o) {
        if (!(o instanceof GeometryValue otherCoord)) {
            throw new IllegalStateException("Cannot compare " + this.getClass().getSimpleName() + " with " + o.getClass().getSimpleName());
        }

        return wktValue.compareTo(otherCoord.wktValue);
    }

    @Override
    public ValueType getValueType() {
        return ValueType.GEOMETRY;
    }

    @Override
    public String toString() {
        return new ObjectMapper().writeValueAsString(this);
    }
}
