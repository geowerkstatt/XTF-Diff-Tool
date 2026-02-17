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
import com.vividsolutions.jts.geom.Geometry;
import org.jspecify.annotations.NonNull;

public final class GeometryValue extends Value {

    @JsonValue
    private final String wktValue;

    GeometryValue(String wktValue) {
        this.wktValue = wktValue;
    }

    /**
     * Create a {@link GeometryValue} from an {@link IomObject} attribute.
     * @see ValueFactory.CreateValue#createValue(IomObject, String, Integer, Type, ValueFactory) ValueFactory.CreateValue
     */
    public static GeometryValue createValue(IomObject obj, String attributeName, Integer index, Type type, ValueFactory factory) {
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
                return null;
            } else {
                var wktValue = new WKTWriterJtsext(3).write(geometry);
                return new GeometryValue(wktValue);
            }
        } catch (IoxException | Iox2jtsException e) {
            throw new RuntimeException(e);
        }
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
}
