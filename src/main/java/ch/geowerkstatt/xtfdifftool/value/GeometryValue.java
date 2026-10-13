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
            Geometry geometry = switch (type.resolveAliases()) {
                case MultiCoordType _ -> Iox2jts.multicoord2JTS(geometryObject);
                case CoordType _ -> (new JtsextGeometryFactory()).createPoint(Iox2jtsext.coord2JTS(geometryObject));
                case PolylineType _ -> Iox2jtsext.polyline2JTS(geometryObject, false, 0);
                case MultiPolylineType _ -> Iox2jts.multipolyline2JTS(geometryObject, 0);
                case SurfaceOrAreaType _ -> Iox2jtsext.surface2JTS(geometryObject, 0);
                case MultiSurfaceOrAreaType _ -> Iox2jtsext.multisurface2JTS(geometryObject, 0, new OutParam<>(), new LogEventFactory(), 0.0, "warning");
                case null, default -> null;
            };

            if (geometry == null) {
                return Optional.empty();
            } else {
                var wktValue = new WKTWriterJtsext(3).write(geometry);
                var location = factory.includesLocations() ? createLocation(geometryObject, type) : null;
                return Optional.of(new GeometryValue(wktValue, location));
            }
        } catch (IoxException | Iox2jtsException e) {
            throw new RuntimeException(e);
        }
    }

    // The geometry as a simple feature every map reads: arcs approximated within the tolerance, in 2D, rounded to mm.
    private static String createLocation(IomObject geometryObject, Type type) throws IoxException, Iox2jtsException {
        Geometry geometry = switch (type.resolveAliases()) {
            case MultiCoordType _ -> Iox2jts.multicoord2JTS(geometryObject);
            case CoordType _ -> LOCATION_FACTORY.createPoint(Iox2jts.coord2JTS(geometryObject));
            case PolylineType _ -> Iox2jts.polyline2JTSlineString(geometryObject, false, LOCATION_ARC_TOLERANCE);
            case MultiPolylineType _ -> Iox2jts.multipolyline2JTS(geometryObject, LOCATION_ARC_TOLERANCE);
            case SurfaceOrAreaType _ -> Iox2jts.surface2JTS(geometryObject, LOCATION_ARC_TOLERANCE);
            case MultiSurfaceOrAreaType _ -> Iox2jts.multisurface2JTS(geometryObject, LOCATION_ARC_TOLERANCE, 0);
            case null, default -> null;
        };

        if (geometry == null) {
            return null;
        }

        var rounded = LOCATION_FACTORY.createGeometry(geometry);
        rounded.apply((CoordinateFilter) LOCATION_PRECISION::makePrecise);
        rounded.geometryChanged();
        return new WKTWriter().write(rounded);
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
