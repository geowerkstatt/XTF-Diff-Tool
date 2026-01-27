package ch.geowerkstatt.xtfdifftool;

import ch.interlis.ili2c.metamodel.AbstractClassDef;
import ch.interlis.ili2c.metamodel.Domain;
import ch.interlis.ili2c.metamodel.Element;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom.IomObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public final class ModelValidator {
    private static final Logger LOGGER = LogManager.getLogger();
    private final TransferDescription transferDescription;
    private final Map<String, Boolean> validatedClasses = new HashMap<>();

    /**
     * Creates a new ModelValidator for the given transfer description.
     * @param transferDescription The INTERLIS transfer description.
     */
    public ModelValidator(TransferDescription transferDescription) {
        this.transferDescription = transferDescription;
    }

    /**
     * Validates that the INTERLIS class of the given IomObject has a stable OID.
     * @param iomObject The IomObject to validate.
     * @return True if the class has a stable OID.
     */
    public boolean validateObjectHasStableOid(IomObject iomObject) {
        return validatedClasses.computeIfAbsent(iomObject.getobjecttag(), this::validateClassHasStableOid);
    }

    private boolean validateClassHasStableOid(String className) {
        Element classElement = transferDescription.getElement(className);
        if (!(classElement instanceof AbstractClassDef<?> classDef)) {
            LOGGER.error("Class or Association \"{}\" not found.", className);
            return false;
        }

        Domain oid = classDef.getOid();
        if (oid == null) {
            LOGGER.warn("Class or Association \"{}\" has no stable OID.", className);
            return false;
        }

        return true;
    }
}
