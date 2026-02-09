package ch.geowerkstatt.xtfdifftool;

import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.config.FileEntry;
import ch.interlis.ili2c.config.FileEntryKind;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.Iom_jObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public final class ObjectPoolTest {
    private static final String TOPIC_ASSOCIATIONS = "Model.TopicAssociations";

    private static final String MODEL_FILE = "src/test/data/ModelValidatorTest/Model.ili";
    private TransferDescription transferDescription;

    @BeforeEach
    public void setUp() {
        Configuration config = new Configuration();
        config.addFileEntry(new FileEntry(MODEL_FILE, FileEntryKind.ILIMODELFILE));
        transferDescription = ch.interlis.ili2c.Main.runCompiler(config);
    }

    @Test
    public void EmbeddedAssociation() {
        List<IomObject> objects = List.of(
                new Iom_jObject(TOPIC_ASSOCIATIONS + ".Main", "oMain"),
                IomObjectHelper.createObject(TOPIC_ASSOCIATIONS + ".A", "oA1", obj -> {
                    obj.addattrobj("RoleMain", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMain");
                    }));
                }),
                IomObjectHelper.createObject(TOPIC_ASSOCIATIONS + ".A", "oA2", obj -> {
                    obj.addattrobj("RoleMain", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMain");
                    }));
                })
        );

        var pool = new ObjectPool(objects.stream(), transferDescription);
        assertAll(
                () -> assertThat(pool.getAssociations("oMain")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleA", List.of("oA1", "oA2"))),
                () -> assertThat(pool.getAssociations("oA1")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"))),
                () -> assertThat(pool.getAssociations("oA2")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"))));
    }

    @Test
    public void EmbeddedAssociationWithAttributes() {
        List<IomObject> objects = List.of(
                new Iom_jObject(TOPIC_ASSOCIATIONS + ".Main", "oMain"),
                IomObjectHelper.createObject(TOPIC_ASSOCIATIONS + ".B", "oB", obj -> {
                    obj.addattrobj("RoleMain", IomObjectHelper.createObject(TOPIC_ASSOCIATIONS + ".EmbeddedWithAttribute", null, ass -> {
                        ass.setobjectrefoid("oMain");
                        ass.setattrvalue("Attr", "ELEPHANT");
                    }));
                })
        );

        var pool = new ObjectPool(objects.stream(), transferDescription);
        assertAll(
                () -> assertThat(pool.getAssociations("oMain")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleB", List.of("oB"))),
                () -> assertThat(pool.getAssociations("oB")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"))));
    }

    @Test
    public void StandaloneAssociation() {
        List<IomObject> objects = List.of(
                new Iom_jObject(TOPIC_ASSOCIATIONS + ".Main", "oMain"),
                new Iom_jObject(TOPIC_ASSOCIATIONS + ".C", "oC"),
                new Iom_jObject(TOPIC_ASSOCIATIONS + ".D", "oD1"),
                new Iom_jObject(TOPIC_ASSOCIATIONS + ".D", "oD2"),
                IomObjectHelper.createObject(TOPIC_ASSOCIATIONS + ".Standalone", null, obj -> {
                    obj.addattrobj("RoleMain", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMain");
                    }));
                    obj.addattrobj("RoleC", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oC");
                    }));
                    obj.addattrobj("RoleD", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oD1");
                    }));
                    obj.addattrobj("RoleD", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oD2");
                    }));
                })
        );

        var pool = new ObjectPool(objects.stream(), transferDescription);
        assertAll(
                () -> assertThat(pool.getAssociations("oMain")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleC", List.of("oC"), "RoleD", List.of("oD1", "oD2"))),
                () -> assertThat(pool.getAssociations("oC")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"), "RoleD", List.of("oD1", "oD2"))),
                () -> assertThat(pool.getAssociations("oD1")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"), "RoleC", List.of("oC"))),
                () -> assertThat(pool.getAssociations("oD2")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"), "RoleC", List.of("oC"))));
    }

    @Test
    public void StandaloneAssociationWithOid() {
        List<IomObject> objects = List.of(
                new Iom_jObject(TOPIC_ASSOCIATIONS + ".Main", "oMain"),
                new Iom_jObject(TOPIC_ASSOCIATIONS + ".E", "oE1"),
                new Iom_jObject(TOPIC_ASSOCIATIONS + ".E", "oE2"),
                IomObjectHelper.createObject(TOPIC_ASSOCIATIONS + ".StandaloneWithOid", "oAssoc1", obj -> {
                    obj.addattrobj("RoleMain", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMain");
                    }));
                    obj.addattrobj("RoleE", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oE1");
                    }));
                }),
                IomObjectHelper.createObject(TOPIC_ASSOCIATIONS + ".StandaloneWithOid", "oAssoc2", obj -> {
                    obj.addattrobj("RoleMain", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMain");
                    }));
                    obj.addattrobj("RoleE", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oE2");
                    }));
                })
        );

        var pool = new ObjectPool(objects.stream(), transferDescription);
        assertAll(
                () -> assertThat(pool.getAssociations("oMain")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleE", List.of("oE1", "oE2"))),
                () -> assertThat(pool.getAssociations("oE1")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"))),
                () -> assertThat(pool.getAssociations("oE2")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Model.TopicMissingOid.ClassOid",
            "Model.TopicMissingOid.AssocOid",
            "Model.TopicOid.ClassOid",
            "Model.TopicOid.ClassMissingOid",
            "Model.TopicOidExtended.ClassMissingOidExtended",
            "Model.TopicOidExtended.ClassOidExtended",
    })
    public void validateClassWithOid(String className) {
        assertTrue(validateObjectHasStableOid(className));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Model.TopicMissingOid.ClassMissingOid",
            "Model.TopicMissingOid.ClassNoOid",
            "Model.TopicOid.ClassNoOid",
            "Model.TopicOid.AssocMissingOid",
            "Model.TopicOidExtended.ClassNoOidExtended",
    })
    public void validateClassWithoutOid(String className) {
        assertFalse(validateObjectHasStableOid(className));
    }

    private boolean validateObjectHasStableOid(String className) {
        IomObject object = new Iom_jObject(className, "o1");
        var pool = new ObjectPool(Stream.of(object), transferDescription);
        return pool.objectsWithStableOid().count() == 1L;
    }
}
