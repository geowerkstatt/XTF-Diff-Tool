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
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public final class ObjectPoolTest {
    private static final String TOPIC_ASSOCIATIONS = "Model.TopicAssociations";
    private static final String TOPIC_BASE_WITHOUT_OID = "Model.TopicBaseWithoutOid";
    private static final String TOPIC_EXTENDED_WITH_OID = "Model.TopicExtendedWithOid";

    private static final String MODEL_FILE = "src/test/data/ObjectPoolTest/Model.ili";
    private TransferDescription transferDescription;

    @BeforeEach
    public void setUp() {
        Configuration config = new Configuration();
        config.addFileEntry(new FileEntry(MODEL_FILE, FileEntryKind.ILIMODELFILE));
        transferDescription = ch.interlis.ili2c.Main.runCompiler(config);
    }

    @Test
    public void embeddedAssociation() {
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
                () -> assertThat(getAssociations(pool, "oMain")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleA", List.of("oA1", "oA2"))),
                () -> assertThat(getAssociations(pool, "oA1")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"))),
                () -> assertThat(getAssociations(pool, "oA2")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"))));
    }

    @Test
    public void embeddedAssociationWithAttributes() {
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
                () -> assertThat(getAssociations(pool, "oMain")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleB", List.of("oB"))),
                () -> assertThat(getAssociations(pool, "oB")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"))));
    }

    @Test
    public void standaloneAssociation() {
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
                () -> assertThat(getAssociations(pool, "oMain")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleC", List.of("oC"), "RoleD", List.of("oD1", "oD2"))),
                () -> assertThat(getAssociations(pool, "oC")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"), "RoleD", List.of("oD1", "oD2"))),
                () -> assertThat(getAssociations(pool, "oD1")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"), "RoleC", List.of("oC"))),
                () -> assertThat(getAssociations(pool, "oD2")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"), "RoleC", List.of("oC"))));
    }

    @Test
    public void standaloneAssociationWithOid() {
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
                () -> assertThat(getAssociations(pool, "oMain")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleE", List.of("oE1", "oE2"))),
                () -> assertThat(getAssociations(pool, "oE1")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"))),
                () -> assertThat(getAssociations(pool, "oE2")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"))));
    }

    @Test
    public void standaloneAssociationWithRoleToUnstableId() {
        List<IomObject> objects = List.of(
                new Iom_jObject(TOPIC_ASSOCIATIONS + ".Main", "oMain"),
                new Iom_jObject(TOPIC_ASSOCIATIONS + ".F", "oUnstableF"),
                new Iom_jObject(TOPIC_ASSOCIATIONS + ".G", "oG"),
                IomObjectHelper.createObject(TOPIC_ASSOCIATIONS + ".StandaloneWithRoleToUnstableId", "oAssoc", obj -> {
                    obj.addattrobj("RoleMain", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMain");
                    }));
                    obj.addattrobj("RoleF", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oUnstableF");
                    }));
                    obj.addattrobj("RoleG", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oG");
                    }));
                })
        );

        var pool = new ObjectPool(objects.stream(), transferDescription);
        assertAll(
                () -> assertThat(getAssociations(pool, "oMain")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleG", List.of("oG"))),
                () -> assertThat(getAssociations(pool, "oUnstableF")).containsExactlyInAnyOrderEntriesOf(Map.of()),
                () -> assertThat(getAssociations(pool, "oG")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMain"))));
    }

    @Test
    public void embeddedAssociationDefinedInBaseTopicWithoutStableOid() {
        List<IomObject> objects = List.of(
                new Iom_jObject(TOPIC_EXTENDED_WITH_OID + ".BaseMain", "oMain"),
                IomObjectHelper.createObject(TOPIC_EXTENDED_WITH_OID + ".BaseA", "oA", obj -> {
                    obj.addattrobj("RoleBaseMain", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMain");
                    }));
                })
        );

        var pool = new ObjectPool(objects.stream(), transferDescription);
        assertAll(
                () -> assertThat(getAssociations(pool, "oMain")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleBaseA", List.of("oA"))),
                () -> assertThat(getAssociations(pool, "oA")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleBaseMain", List.of("oMain"))));
    }

    @Test
    public void standaloneAssociationDefinedInBaseTopicWithoutStableOid() {
        List<IomObject> objects = List.of(
                new Iom_jObject(TOPIC_EXTENDED_WITH_OID + ".BaseMain", "oMain"),
                new Iom_jObject(TOPIC_EXTENDED_WITH_OID + ".BaseB", "oB"),
                IomObjectHelper.createObject(TOPIC_BASE_WITHOUT_OID + ".BaseStandalone", null, obj -> {
                    obj.addattrobj("RoleBaseMain2", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMain");
                    }));
                    obj.addattrobj("RoleBaseB", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oB");
                    }));
                })
        );

        var pool = new ObjectPool(objects.stream(), transferDescription);
        assertAll(
                () -> assertThat(getAssociations(pool, "oMain")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleBaseB", List.of("oB"))),
                () -> assertThat(getAssociations(pool, "oB")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleBaseMain2", List.of("oMain"))));
    }

    @Test
    public void associationReferencingObjectWithoutStableOidInTransferIsIgnored() {
        List<IomObject> objects = List.of(
                new Iom_jObject(TOPIC_EXTENDED_WITH_OID + ".BaseMain", "oMain"),
                new Iom_jObject(TOPIC_BASE_WITHOUT_OID + ".BaseB", "oUnstableB"),
                IomObjectHelper.createObject(TOPIC_BASE_WITHOUT_OID + ".BaseA", "oUnstableA", obj -> {
                    obj.addattrobj("RoleBaseMain", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMain");
                    }));
                }),
                IomObjectHelper.createObject(TOPIC_BASE_WITHOUT_OID + ".BaseStandalone", null, obj -> {
                    obj.addattrobj("RoleBaseMain2", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMain");
                    }));
                    obj.addattrobj("RoleBaseB", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oUnstableB");
                    }));
                })
        );

        var pool = new ObjectPool(objects.stream(), transferDescription);
        assertAll(
                () -> assertThat(getAssociations(pool, "oMain")).containsExactlyInAnyOrderEntriesOf(Map.of()),
                () -> assertThat(getAssociations(pool, "oUnstableA")).containsExactlyInAnyOrderEntriesOf(Map.of()),
                () -> assertThat(getAssociations(pool, "oUnstableB")).containsExactlyInAnyOrderEntriesOf(Map.of()),
                () -> assertThat(getCounts(pool, ObjectPool.ReferenceOutcome.IGNORED_UNSTABLE_IN_TRANSFER)).containsExactlyInAnyOrderEntriesOf(Map.of(
                        TOPIC_BASE_WITHOUT_OID + ".BaseEmbedded.RoleBaseA", 1,
                        TOPIC_BASE_WITHOUT_OID + ".BaseStandalone.RoleBaseB", 1)));
    }

    @Test
    public void roleWithOnlyUnstableTargetsReferencingMissingObjectIsCompared() {
        // The role F can only target the class F without stable OID. A reference to an object that
        // is not part of the transfer is still compared, because the referenced TID might be stable.
        List<IomObject> objects = List.of(
                new Iom_jObject(TOPIC_ASSOCIATIONS + ".Main", "oMain"),
                IomObjectHelper.createObject(TOPIC_ASSOCIATIONS + ".StandaloneWithRoleToUnstableId", "oAssoc", obj -> {
                    obj.addattrobj("RoleMain", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMain");
                    }));
                    obj.addattrobj("RoleF", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMissingF");
                    }));
                })
        );

        var pool = new ObjectPool(objects.stream(), transferDescription);
        assertAll(
                () -> assertThat(getAssociations(pool, "oMain")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleF", List.of("oMissingF"))),
                () -> assertThat(getCounts(pool, ObjectPool.ReferenceOutcome.COMPARED_MAYBE_UNSTABLE)).containsExactlyInAnyOrderEntriesOf(Map.of(
                        TOPIC_ASSOCIATIONS + ".StandaloneWithRoleToUnstableId.RoleF", 1)));
    }

    @Test
    public void associationReferencingObjectMissingInTransferIsCompared() {
        // The classes Main and C have a stable OID according to the model, the references are
        // compared without warning although the referenced objects are not part of the transfer.
        List<IomObject> objects = List.of(
                new Iom_jObject(TOPIC_ASSOCIATIONS + ".Main", "oMain"),
                IomObjectHelper.createObject(TOPIC_ASSOCIATIONS + ".A", "oA", obj -> {
                    obj.addattrobj("RoleMain", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMissingMain");
                    }));
                }),
                IomObjectHelper.createObject(TOPIC_ASSOCIATIONS + ".Standalone", null, obj -> {
                    obj.addattrobj("RoleMain", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMain");
                    }));
                    obj.addattrobj("RoleC", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMissingC");
                    }));
                })
        );

        var pool = new ObjectPool(objects.stream(), transferDescription);
        assertAll(
                () -> assertThat(getAssociations(pool, "oMain")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleC", List.of("oMissingC"))),
                () -> assertThat(getAssociations(pool, "oA")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleMain", List.of("oMissingMain"))),
                () -> assertThat(getCounts(pool, ObjectPool.ReferenceOutcome.IGNORED_NO_STABLE_ENDPOINT)).isEmpty(),
                () -> assertThat(getCounts(pool, ObjectPool.ReferenceOutcome.IGNORED_UNSTABLE_IN_TRANSFER)).isEmpty(),
                () -> assertThat(getCounts(pool, ObjectPool.ReferenceOutcome.COMPARED_MAYBE_UNSTABLE)).isEmpty());
    }

    @Test
    public void extendedClassReferencingMissingObjectIsCompared() {
        // Mirrors a catalogue reference: the referencing object belongs to an extended class in a
        // topic with stable OIDs, the role is defined in the base topic without stable OIDs, and
        // the referenced object (e.g. a catalogue entry) is not part of the transfer.
        List<IomObject> objects = List.of(
                IomObjectHelper.createObject(TOPIC_EXTENDED_WITH_OID + ".BaseA", "oA", obj -> {
                    obj.addattrobj("RoleBaseMain", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMissingMain");
                    }));
                })
        );

        var pool = new ObjectPool(objects.stream(), transferDescription);
        assertAll(
                () -> assertThat(getAssociations(pool, "oA")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleBaseMain", List.of("oMissingMain"))),
                () -> assertThat(getCounts(pool, ObjectPool.ReferenceOutcome.COMPARED_MAYBE_UNSTABLE)).containsExactlyInAnyOrderEntriesOf(Map.of(
                        TOPIC_BASE_WITHOUT_OID + ".BaseEmbedded.RoleBaseMain", 1)));
    }

    @Test
    public void externalRoleReferencingMissingObjectIsCompared() {
        // Same as above but with an EXTERNAL role, as used by catalogue references.
        List<IomObject> objects = List.of(
                IomObjectHelper.createObject(TOPIC_EXTENDED_WITH_OID + ".BaseB", "oB", obj -> {
                    obj.addattrobj("RoleExternalMain", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMissingCatalogueEntry");
                    }));
                })
        );

        var pool = new ObjectPool(objects.stream(), transferDescription);
        assertAll(
                () -> assertThat(getAssociations(pool, "oB")).containsExactlyInAnyOrderEntriesOf(Map.of("RoleExternalMain", List.of("oMissingCatalogueEntry"))),
                () -> assertThat(getCounts(pool, ObjectPool.ReferenceOutcome.COMPARED_MAYBE_UNSTABLE)).containsExactlyInAnyOrderEntriesOf(Map.of(
                        TOPIC_BASE_WITHOUT_OID + ".BaseExternal.RoleExternalMain", 1)));
    }

    @Test
    public void associationBetweenObjectsWithoutStableOidIsIgnored() {
        // No involved object has a stable OID (e.g. a topic without any OID definition):
        // there is no object to attach a comparison to, the references are ignored entirely.
        List<IomObject> objects = List.of(
                new Iom_jObject(TOPIC_BASE_WITHOUT_OID + ".BaseMain", "oUnstableMain"),
                new Iom_jObject(TOPIC_BASE_WITHOUT_OID + ".BaseB", "oUnstableB"),
                IomObjectHelper.createObject(TOPIC_BASE_WITHOUT_OID + ".BaseA", "oUnstableA", obj -> {
                    obj.addattrobj("RoleBaseMain", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oUnstableMain");
                    }));
                }),
                IomObjectHelper.createObject(TOPIC_BASE_WITHOUT_OID + ".BaseA", "oUnstableA2", obj -> {
                    obj.addattrobj("RoleBaseMain", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMissingMain");
                    }));
                }),
                IomObjectHelper.createObject(TOPIC_BASE_WITHOUT_OID + ".BaseStandalone", null, obj -> {
                    obj.addattrobj("RoleBaseMain2", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oUnstableMain");
                    }));
                    obj.addattrobj("RoleBaseB", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oUnstableB");
                    }));
                })
        );

        var pool = new ObjectPool(objects.stream(), transferDescription);
        assertAll(
                () -> assertEquals(0, pool.objectsWithStableOid().count()),
                () -> assertThat(getCounts(pool, ObjectPool.ReferenceOutcome.IGNORED_NO_STABLE_ENDPOINT)).containsExactlyInAnyOrderEntriesOf(Map.of(
                        TOPIC_BASE_WITHOUT_OID + ".BaseEmbedded.RoleBaseMain", 2,
                        TOPIC_BASE_WITHOUT_OID + ".BaseEmbedded.RoleBaseA", 2,
                        TOPIC_BASE_WITHOUT_OID + ".BaseStandalone.RoleBaseMain2", 1,
                        TOPIC_BASE_WITHOUT_OID + ".BaseStandalone.RoleBaseB", 1)),
                () -> assertThat(getCounts(pool, ObjectPool.ReferenceOutcome.IGNORED_UNSTABLE_IN_TRANSFER)).isEmpty(),
                () -> assertThat(getCounts(pool, ObjectPool.ReferenceOutcome.COMPARED_MAYBE_UNSTABLE)).isEmpty());
    }

    @Test
    public void objectOfUnknownClassIsTreatedLikeObjectWithoutStableOid() {
        // The class of oUnknown is not part of the compiled model: the object cannot join the
        // pool, and references to its TID are ignored like references to unstable objects.
        List<IomObject> objects = List.of(
                new Iom_jObject(TOPIC_ASSOCIATIONS + ".Main", "oMain"),
                new Iom_jObject(TOPIC_ASSOCIATIONS + ".DoesNotExist", "oUnknown"),
                IomObjectHelper.createObject(TOPIC_ASSOCIATIONS + ".Standalone", null, obj -> {
                    obj.addattrobj("RoleMain", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oMain");
                    }));
                    obj.addattrobj("RoleC", IomObjectHelper.createObject(Iom_jObject.REF, null, ass -> {
                        ass.setobjectrefoid("oUnknown");
                    }));
                })
        );

        var pool = new ObjectPool(objects.stream(), transferDescription);
        assertAll(
                () -> assertEquals(1, pool.objectsWithStableOid().count()),
                () -> assertTrue(pool.getObject("oUnknown").isEmpty()),
                () -> assertThat(getAssociations(pool, "oMain")).containsExactlyInAnyOrderEntriesOf(Map.of()),
                () -> assertThat(getCounts(pool, ObjectPool.ReferenceOutcome.IGNORED_UNSTABLE_IN_TRANSFER)).containsExactlyInAnyOrderEntriesOf(Map.of(
                        TOPIC_ASSOCIATIONS + ".Standalone.RoleC", 1)));
    }

    private static Map<String, Integer> getCounts(ObjectPool pool, ObjectPool.ReferenceOutcome outcome) {
        return pool.getReferenceStatistics().getCounts(outcome);
    }

    private static Map<String, List<String>> getAssociations(ObjectPool pool, String tid) {
        var mapper = new ObjectMapper();
        return pool.getObject(tid)
                .<Map<String, List<String>>>map(objectValue -> objectValue
                    .getAssociations().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> mapper.readValue(e.getValue().toString(), new TypeReference<>() {
                })))).orElseGet(Map::of);
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
