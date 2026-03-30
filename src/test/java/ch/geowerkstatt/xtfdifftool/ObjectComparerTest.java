package ch.geowerkstatt.xtfdifftool;

import ch.geowerkstatt.xtfdifftool.diff.ChangeType;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.config.FileEntry;
import ch.interlis.ili2c.config.FileEntryKind;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.Iom_jObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static ch.geowerkstatt.xtfdifftool.IomObjectHelper.createCoord;
import static ch.geowerkstatt.xtfdifftool.IomObjectHelper.createMultiCoord;
import static ch.geowerkstatt.xtfdifftool.IomObjectHelper.createObject;
import static ch.geowerkstatt.xtfdifftool.IomObjectHelper.createRef;
import static org.assertj.core.api.Assertions.assertThat;

public final class ObjectComparerTest {
    private static final String MODEL_FILE = "src/test/data/ObjectComparerTest/Model.ili";
    private static final String TOPIC = "ObjectComparerTest.Topic";
    private static final String CLASS_NAME = TOPIC + ".Class";
    private static final String CLASS_NAME_B = TOPIC + ".ClassB";
    private static final String CLASS_NAME_WITHOUT_ID = TOPIC + ".ClassWithoutId";
    private static final String STRUCT_NAME = TOPIC + ".Struct";
    private static final String EMBEDDED_ASSOCIATION_NAME = TOPIC + ".EmbeddedAssociation";
    private static final String STANDALONE_ASSOCIATION_NAME = TOPIC + ".StandaloneAssociation";
    private static final String GEOMETRY_CLASS = "ObjectComparerTest.Geometry.Class";
    private TransferDescription transferDescription;

    @BeforeEach
    public void setUp() {
        Configuration config = new Configuration();
        config.addFileEntry(new FileEntry(MODEL_FILE, FileEntryKind.ILIMODELFILE));
        transferDescription = ch.interlis.ili2c.Main.runCompiler(config);
    }

    @Test
    public void analyzeNoObjects() {
        assertComparison(List.of(), List.of(), List.of());
    }

    @Test
    public void analyzeSameObjects() {
        List<IomObject> first = List.of(
                createObject(CLASS_NAME, "o1"),
                createObject(CLASS_NAME, "o2"),
                createObject(CLASS_NAME, "o3")
        );
        List<IomObject> second = List.of(
                createObject(CLASS_NAME, "o2"),
                createObject(CLASS_NAME, "o1"),
                createObject(CLASS_NAME, "o3")
        );

        List<TestChange> expectedChanges = Collections.emptyList();

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzeAddedObjects() {
        List<IomObject> first = List.of(
                createObject(CLASS_NAME, "o1")
        );
        List<IomObject> second = List.of(
                createObject(CLASS_NAME, "o1"),
                createObject(CLASS_NAME, "o2"),
                createObject(CLASS_NAME, "o3")
        );

        var expectedChanges = List.of(
                createChange("o2", ChangeType.ADDED),
                createChange("o3", ChangeType.ADDED)
        );

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzeDeletedObjects() {
        List<IomObject> first = List.of(
                createObject(CLASS_NAME, "o1"),
                createObject(CLASS_NAME, "o2"),
                createObject(CLASS_NAME, "o3")
        );
        List<IomObject> second = List.of(
                createObject(CLASS_NAME, "o2")
        );

        var expectedChanges = List.of(
                createChange("o1", ChangeType.DELETED),
                createChange("o3", ChangeType.DELETED)
        );

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzeDifferentObjects() {
        List<IomObject> first = List.of(
                createObject(CLASS_NAME, "o1"),
                createObject(CLASS_NAME, "o2"),
                createObject(CLASS_NAME, "o3")
        );
        List<IomObject> second = List.of(
                createObject(CLASS_NAME, "o4"),
                createObject(CLASS_NAME, "o3"),
                createObject(CLASS_NAME, "o2")
        );

        var expectedChanges = List.of(
                createChange("o1", ChangeType.DELETED),
                createChange("o4", ChangeType.ADDED)
        );

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzeDifferentPrimitiveAttribute() {
        var first = List.of(createObject(CLASS_NAME, "o1", obj -> {
            obj.setattrvalue("text", "Wazuviti");
            obj.setattrvalue("text2", "Unchanged");
            obj.addattrvalue("textList", "A");
            obj.addattrvalue("textList", "B");
        }));
        var second = List.of(createObject(CLASS_NAME, "o1", obj -> {
            obj.setattrvalue("text", "Fazakapo");
            obj.setattrvalue("text2", "Unchanged");
            obj.addattrvalue("textList", "A");
            obj.addattrvalue("textList", "C");
            obj.addattrvalue("textList", "D");
        }));

        var expectedChanges = List.of(
                new TestChange("o1", ChangeType.CHANGED, ValueType.ATTRIBUTE, CLASS_NAME, "text", "\"Wazuviti\"", "\"Fazakapo\""),
                new TestChange("o1", ChangeType.CHANGED, ValueType.ATTRIBUTE, CLASS_NAME, "textList", "[\"A\",\"B\"]", "[\"A\",\"C\",\"D\"]")
        );

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzeDeletePrimitiveAttribute() {
        var first = List.of(createObject(CLASS_NAME, "o1", obj -> {
            obj.setattrvalue("text", "Wazuviti");
            obj.setattrvalue("text2", "Unchanged");
            obj.addattrvalue("textList", "A");
            obj.addattrvalue("textList", "B");
        }));
        var second = List.of(createObject(CLASS_NAME, "o1", obj -> {
            obj.setattrvalue("text2", "Unchanged");
        }));

        var expectedChanges = List.of(
                new TestChange("o1", ChangeType.DELETED, ValueType.ATTRIBUTE, CLASS_NAME, "text", "\"Wazuviti\"", null),
                new TestChange("o1", ChangeType.DELETED, ValueType.ATTRIBUTE, CLASS_NAME, "textList", "[\"A\",\"B\"]", null)
        );

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzeAddedPrimitiveAttribute() {
        var first = List.of(createObject(CLASS_NAME, "o1", obj -> {
            obj.setattrvalue("text2", "Unchanged");
        }));
        var second = List.of(createObject(CLASS_NAME, "o1", obj -> {
            obj.setattrvalue("text", "Fazakapo");
            obj.setattrvalue("text2", "Unchanged");
            obj.addattrvalue("textList", "A");
            obj.addattrvalue("textList", "C");
        }));

        var expectedChanges = List.of(
                new TestChange("o1", ChangeType.ADDED, ValueType.ATTRIBUTE, CLASS_NAME, "text", null, "\"Fazakapo\""),
                new TestChange("o1", ChangeType.ADDED, ValueType.ATTRIBUTE, CLASS_NAME, "textList", null, "[\"A\",\"C\"]")
        );

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzeDifferentPrimitiveCollectionAttribute() {
        var first = List.of(
                createObject(CLASS_NAME, "o1", obj -> {
                    obj.addattrvalue("textList", "A");
                    obj.addattrvalue("textList", "B");
                    obj.addattrvalue("textBag", "C");
                    obj.addattrvalue("textBag", "D");
                }), createObject(CLASS_NAME, "o2", obj -> {
                    obj.addattrvalue("textBag", "S");
                    obj.addattrvalue("textBag", "E");
                    obj.addattrvalue("textBag", "T");
                }));
        var second = List.of(
                createObject(CLASS_NAME, "o1", obj -> {
                    obj.addattrvalue("textList", "B");
                    obj.addattrvalue("textList", "A");
                    obj.addattrvalue("textBag", "D");
                    obj.addattrvalue("textBag", "C");
                }), createObject(CLASS_NAME, "o2", obj -> {
                    obj.addattrvalue("textBag", "T");
                    obj.addattrvalue("textBag", "E");
                    obj.addattrvalue("textBag", "A");
                }));

        var expectedChanges = List.of(
                new TestChange("o1", ChangeType.CHANGED, ValueType.ATTRIBUTE, CLASS_NAME, "textList", "[\"A\",\"B\"]", "[\"B\",\"A\"]"),
                new TestChange("o2", ChangeType.CHANGED, ValueType.ATTRIBUTE, CLASS_NAME, "textBag", "[\"E\",\"S\",\"T\"]", "[\"A\",\"E\",\"T\"]")
        );

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzeRoundNumericAttribute() {
        var first = List.of(
                createObject(CLASS_NAME, "o1", obj -> {
                    obj.setattrvalue("number", "42.4");
                    obj.setattrvalue("numberTwoDecimals", "-1.004");
                }),
                createObject(CLASS_NAME, "o2", obj -> {
                    obj.setattrvalue("number", "-2.5");
                    obj.setattrvalue("numberTwoDecimals", "0.005");
                }),
                createObject(CLASS_NAME, "o3", obj -> {
                    obj.setattrvalue("number", "0.9");
                    obj.setattrvalue("numberTwoDecimals", "6.789");
                })
        );
        var second = List.of(
                createObject(CLASS_NAME, "o1", obj -> {
                    obj.setattrvalue("number", "42");
                    obj.setattrvalue("numberTwoDecimals", "-1");
                }),
                createObject(CLASS_NAME, "o2", obj -> {
                    obj.setattrvalue("number", "-2");
                    obj.setattrvalue("numberTwoDecimals", "0.01");
                }),
                createObject(CLASS_NAME, "o3", obj -> {
                    obj.setattrvalue("number", "9.0");
                    obj.setattrvalue("numberTwoDecimals", "6.8");
                })
        );

        var expectedChanges = List.of(
                new TestChange("o3", ChangeType.CHANGED, ValueType.ATTRIBUTE, CLASS_NAME, "number", "\"1\"", "\"9\""),
                new TestChange("o3", ChangeType.CHANGED, ValueType.ATTRIBUTE, CLASS_NAME, "numberTwoDecimals", "\"6.79\"", "\"6.80\"")
        );

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzeIgnoresObjectsWithoutId() {
        List<IomObject> first = List.of(
                new Iom_jObject(CLASS_NAME_WITHOUT_ID, "123"),
                new Iom_jObject(CLASS_NAME_WITHOUT_ID, "456")
        );
        List<IomObject> second = List.of(
                new Iom_jObject(CLASS_NAME_WITHOUT_ID, "789")
        );

        List<TestChange> expectedChanges = Collections.emptyList();

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzeDifferentStructs() {
        var first = List.of(
                createObject(CLASS_NAME, "o1", obj -> {
                    obj.addattrobj("struct", createObject(STRUCT_NAME, null, struct -> {
                        struct.setattrvalue("text", "A");
                        struct.setattrvalue("text2", "abc");
                    }));
                }), createObject(CLASS_NAME, "o2", obj -> {
                    obj.addattrobj("struct", createObject(STRUCT_NAME, null, struct -> {
                        struct.setattrvalue("text", "B");
                    }));
                }));
        var second = List.of(
                createObject(CLASS_NAME, "o1", obj -> {
                    obj.addattrobj("struct", createObject(STRUCT_NAME, null, struct -> {
                        struct.setattrvalue("text", "C");
                    }));
                }), createObject(CLASS_NAME, "o2", obj -> {
                    obj.addattrobj("struct", createObject(STRUCT_NAME, null, struct -> {
                        struct.setattrvalue("text", "B");
                    }));
                }));

        var expectedChanges = List.of(
                new TestChange("o1", ChangeType.CHANGED, ValueType.ATTRIBUTE, CLASS_NAME, "struct.text", "\"A\"", "\"C\""),
                new TestChange("o1", ChangeType.DELETED, ValueType.ATTRIBUTE, CLASS_NAME, "struct.text2", "\"abc\"", null)
        );

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzeDifferentStructLists() {
        var first = List.of(
                createObject(CLASS_NAME, "o1", obj -> {
                    obj.addattrobj("structList", createObject(STRUCT_NAME, null, struct -> struct.setattrvalue("text", "A")));
                    obj.addattrobj("structList", createObject(STRUCT_NAME, null, struct -> struct.setattrvalue("text", "B")));
                    obj.addattrobj("structList", createObject(STRUCT_NAME, null, struct -> struct.setattrvalue("text", "C")));
                }), createObject(CLASS_NAME, "o2"));
        var second = List.of(
                createObject(CLASS_NAME, "o1", obj -> {
                    obj.addattrobj("structList", createObject(STRUCT_NAME, null, struct -> struct.setattrvalue("text", "A")));
                    obj.addattrobj("structList", createObject(STRUCT_NAME, null, struct -> {
                        struct.setattrvalue("text", "D");
                        struct.setattrvalue("text2", "new value");
                    }));
                }), createObject(CLASS_NAME, "o2", obj -> {
                    obj.addattrobj("structList", createObject(STRUCT_NAME, null, struct -> struct.setattrvalue("text", "A")));
                }));

        var expectedChanges = List.of(
                new TestChange("o1", ChangeType.CHANGED, ValueType.ATTRIBUTE, CLASS_NAME, "structList", """
                        [{"text":"A"},{"text":"B"},{"text":"C"}]""", """
                        [{"text":"A"},{"text":"D","text2":"new value"}]"""),
                new TestChange("o2", ChangeType.ADDED, ValueType.ATTRIBUTE, CLASS_NAME, "structList", null, "[{\"text\":\"A\"}]")
        );

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzeBagOfPolymorphicStructs() {
        final String attribute = "structBag";

        var first = List.of(createObject(CLASS_NAME, "o1", obj -> {
            obj.addattrobj(attribute, createObject(TOPIC + ".DerivedStruct", null, struct -> {
                struct.setattrvalue("text", "Ferret");
                struct.setattrvalue("bool", "true");
            }));
            obj.addattrobj(attribute, createObject(STRUCT_NAME, null, struct -> {
                struct.setattrvalue("text", "Sloth");
            }));
        }));
        var second = List.of(createObject(CLASS_NAME, "o1", obj -> {
            obj.addattrobj(attribute, createObject(STRUCT_NAME, null, struct -> {
                struct.setattrvalue("text", "Sloth");
            }));
            obj.addattrobj(attribute, createObject(TOPIC + ".DerivedStruct", null, struct -> {
                struct.setattrvalue("text", "Ferret");
                struct.setattrvalue("bool", "false"); // value on derived struct changed
            }));
        }));

        var expectedChanges = List.of(new TestChange("o1", ChangeType.CHANGED, ValueType.ATTRIBUTE, CLASS_NAME, attribute, """
                [{"text":"Ferret","bool":"true"},{"text":"Sloth"}]""", """
                [{"text":"Ferret","bool":"false"},{"text":"Sloth"}]"""));

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzeEmbeddedAssociation() {
        var first = List.of(
                createObject(CLASS_NAME, "o1"),
                createObject(CLASS_NAME, "o2"),
                createObject(CLASS_NAME_B, "o3", obj -> {
                    var role = createObject(EMBEDDED_ASSOCIATION_NAME, null, assoc -> {
                        assoc.setobjectrefoid("o1");
                        assoc.setattrvalue("value", "ABC");
                    });
                    obj.addattrobj("role1A", role);
                }),
                createObject(CLASS_NAME_B, "o4", obj -> {
                    var role = createRef("o2");
                    obj.addattrobj("role1A", role);
                }),
                createObject(CLASS_NAME_B, "o5", obj -> {
                    var role = createObject(EMBEDDED_ASSOCIATION_NAME, null, assoc -> {
                        assoc.setobjectrefoid("o2");
                        assoc.setattrvalue("value", "some text");
                    });
                    obj.addattrobj("role1A", role);
                }),
                createObject(CLASS_NAME_B, "o6", obj -> {
                    var role = createRef("o2");
                    obj.addattrobj("role1A", role);
                })
        );
        var second = List.of(
                createObject(CLASS_NAME, "o1"),
                createObject(CLASS_NAME, "o2"),
                createObject(CLASS_NAME_B, "o3", obj -> {
                    var role = createObject(EMBEDDED_ASSOCIATION_NAME, null, assoc -> {
                        assoc.setobjectrefoid("o1");
                        assoc.setattrvalue("value", "ABC");
                    });
                    obj.addattrobj("role1A", role);
                }),
                createObject(CLASS_NAME_B, "o4", obj -> {
                    var role = createRef("o1");
                    obj.addattrobj("role1A", role);
                }),
                createObject(CLASS_NAME_B, "o5", obj -> {
                    var role = createObject(EMBEDDED_ASSOCIATION_NAME, null, assoc -> {
                        assoc.setobjectrefoid("o2");
                        assoc.setattrvalue("value", "some other text");
                    });
                    obj.addattrobj("role1A", role);
                }),
                createObject(CLASS_NAME_B, "o6", obj -> {
                    var role = createObject(EMBEDDED_ASSOCIATION_NAME, null, assoc -> {
                        assoc.setobjectrefoid("o2");
                        assoc.setattrvalue("value", "new value");
                    });
                    obj.addattrobj("role1A", role);
                })
        );

        var expectedChanges = List.of(
                new TestChange("o1", ChangeType.ADDED, ValueType.REFERENCE, CLASS_NAME, "role1B", null, "[\"o4\"]"),
                new TestChange("o2", ChangeType.DELETED, ValueType.REFERENCE, CLASS_NAME, "role1B", "[\"o4\"]", null),
                new TestChange("o4", ChangeType.CHANGED, ValueType.REFERENCE, CLASS_NAME_B, "role1A", "[\"o2\"]", "[\"o1\"]")
        );

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzeReferenceAttribute() {
        var first = List.of(
                createObject(CLASS_NAME, "o1"),
                createObject(CLASS_NAME, "o2"),
                createObject(CLASS_NAME_B, "o3", obj -> {
                    obj.addattrobj("ref", createRef("o1"));
                }),
                createObject(CLASS_NAME_B, "o4", obj -> {
                    obj.addattrobj("ref", createRef("o1"));
                })
        );
        var second = List.of(
                createObject(CLASS_NAME, "o1"),
                createObject(CLASS_NAME, "o2"),
                createObject(CLASS_NAME_B, "o3", obj -> {
                    obj.addattrobj("ref", createRef("o1"));
                }),
                createObject(CLASS_NAME_B, "o4", obj -> {
                    obj.addattrobj("ref", createRef("o2"));
                })
        );

        var expectedChanges = List.of(
                new TestChange("o4", ChangeType.CHANGED, ValueType.REFERENCE, CLASS_NAME_B, "ref", "\"o1\"", "\"o2\"")
        );

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzeStandaloneAssociation() {
        var first = List.of(
                createObject(CLASS_NAME, "oA1"),
                createObject(CLASS_NAME_B, "oB1"),
                createObject(CLASS_NAME_B, "oB2"),
                createObject(STANDALONE_ASSOCIATION_NAME, "oAssoc1", obj -> {
                    obj.addattrobj("roleA", createRef("oA1"));
                    obj.addattrobj("roleB", createRef("oB1"));
                    obj.addattrvalue("value", "SLUG");
                }),
                createObject(STANDALONE_ASSOCIATION_NAME, "oAssoc2", obj -> {
                    obj.addattrobj("roleA", createRef("oA1"));
                    obj.addattrobj("roleB", createRef("oB2"));
                    obj.addattrvalue("value", "DODO");
                })
        );
        var second = List.of(
                createObject(CLASS_NAME, "oA1"),
                createObject(CLASS_NAME_B, "oB2"),
                createObject(CLASS_NAME_B, "oB3"),
                createObject(STANDALONE_ASSOCIATION_NAME, "oAssoc1", obj -> {
                    obj.addattrobj("roleA", createRef("oA1"));
                    obj.addattrobj("roleB", createRef("oB3"));
                    obj.addattrvalue("value", "SLUG");
                }),
                createObject(STANDALONE_ASSOCIATION_NAME, "oAssoc2", obj -> {
                    obj.addattrobj("roleA", createRef("oA1"));
                    obj.addattrobj("roleB", createRef("oB2"));
                    obj.addattrvalue("value", "MOTH");
                })
        );

        var expectedChanges = List.of(
                new TestChange("oB1", ChangeType.DELETED, ValueType.OBJECT, CLASS_NAME_B, null, null, null),
                new TestChange("oB3", ChangeType.ADDED, ValueType.OBJECT, CLASS_NAME_B, null, null, null),
                new TestChange("oA1", ChangeType.DELETED, ValueType.REFERENCE, CLASS_NAME, "roleB", "[\"oB1\"]", null),
                new TestChange("oA1", ChangeType.ADDED, ValueType.REFERENCE, CLASS_NAME, "roleB", null, "[\"oB3\"]"),
                new TestChange("oAssoc2", ChangeType.CHANGED, ValueType.ATTRIBUTE, STANDALONE_ASSOCIATION_NAME, "value", "\"DODO\"", "\"MOTH\"")
        );

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzePointChange() {
        final String attribute = "Point3";

        var first = List.of(createObject(GEOMETRY_CLASS, "o1", obj -> {
            obj.addattrobj(attribute, createCoord("20", "30", "54"));
        }));
        var second = List.of(createObject(GEOMETRY_CLASS, "o1", obj -> {
            obj.addattrobj(attribute, createCoord("20", "30", "59"));
        }));

        var expectedChanges = List.of(new TestChange("o1", ChangeType.CHANGED, ValueType.GEOMETRY, GEOMETRY_CLASS, attribute, """
                "POINT (20 30 54)\"""", """
                "POINT (20 30 59)\""""));

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzePointListChange() {
        final String attribute = "Point3List";

        var first = List.of(createObject(GEOMETRY_CLASS, "o1", obj -> {
            obj.addattrobj(attribute, createCoord("20", "30", "54"));
            obj.addattrobj(attribute, createCoord("10", "15", "27"));
        }));
        var second = List.of(createObject(GEOMETRY_CLASS, "o1", obj -> {
            obj.addattrobj(attribute, createCoord("10", "15", "27"));
            obj.addattrobj(attribute, createCoord("20", "30", "54"));
        }));

        var expectedChanges = List.of(new TestChange("o1", ChangeType.CHANGED, ValueType.GEOMETRY, GEOMETRY_CLASS, attribute, """
                ["POINT (20 30 54)","POINT (10 15 27)"]""", """
                ["POINT (10 15 27)","POINT (20 30 54)"]"""));

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzePointBagChange() {
        final String attribute = "Point3Bag";

        var first = List.of(createObject(GEOMETRY_CLASS, "o1", obj -> {
            obj.addattrobj(attribute, createCoord("20", "30", "54"));
            obj.addattrobj(attribute, createCoord("10", "15", "27"));
            obj.addattrobj(attribute, createCoord("30", "45", "81"));
        }));
        var second = List.of(createObject(GEOMETRY_CLASS, "o1", obj -> {
            obj.addattrobj(attribute, createCoord("9.995", "15", "27"));
            obj.addattrobj(attribute, createCoord("30.0049", "45", "81"));
            obj.addattrobj(attribute, createCoord("20", "30", "54"));
        }));

        List<TestChange> expectedChanges = Collections.emptyList();

        assertComparison(first, second, expectedChanges);
    }

    @Test
    public void analyzeMultiPointChange() {
        final String attribute = "MultiPoint3";

        var first = List.of(createObject(GEOMETRY_CLASS, "o1", obj -> {
            obj.addattrobj(attribute, createMultiCoord(
                    createCoord("20", "30", "54"),
                    createCoord("10", "15", "27"),
                    createCoord("30", "45", "81")));
        }));
        var second = List.of(createObject(GEOMETRY_CLASS, "o1", obj -> {
            obj.addattrobj(attribute, createMultiCoord(
                    createCoord("9.995", "15", "27"),
                    createCoord("30", "45", "81")));
        }));

        var expectedChanges = List.of(new TestChange("o1", ChangeType.CHANGED, ValueType.GEOMETRY, GEOMETRY_CLASS, attribute, """
                "MULTIPOINT ((20 30 54), (10 15 27), (30 45 81))\"""", """
                "MULTIPOINT ((9.995 15 27), (30 45 81))\""""));

        assertComparison(first, second, expectedChanges);
    }

    private TestChange createChange(String oid, ChangeType type) {
        return new TestChange(oid, type, ValueType.OBJECT, CLASS_NAME, null, null, null);
    }

    private void assertComparison(List<IomObject> first, List<IomObject> second, List<TestChange> expectedChanges) {
        ObjectComparer objectComparer = new ObjectComparer(transferDescription, first.stream(), second.stream());
        var changes = objectComparer.analyzeDifferences().map(TestChange::new).toList();
        assertThat(changes).containsExactlyInAnyOrderElementsOf(expectedChanges);
    }
}
