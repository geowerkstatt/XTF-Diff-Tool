# JSON-Dokumentation für XTF-Diff-Tool

Dieses Dokument beschreibt die Struktur und Bedeutung der Eigenschaften im JSON-Ausgabeformat des XTF-Diff-Tools. 

## Struktur des JSON-Objekts
Jedes Element im JSON-Array repräsentiert eine Änderung an einem Objekt und enthält folgende Eigenschaften:

| Eigenschaft      | Typ / Werte                        | Beschreibung                                                                 |
|------------------|------------------------------------|------------------------------------------------------------------------------|
| oid              | UUID                               | Eindeutige Transfer-ID des betroffenen Objekts                                          |
| changeType       | "changed" &#124; "added" &#124; "deleted" | Typ der Änderung (hinzugefügt, geändert, gelöscht)                           |
| valueType        | "object" &#124; "reference" &#124; "attribute" &#124; "geometry" | Typ der geänderten Objekteigenschaft                                         |
| interlisName     | String                             | Vollqualifizierter Name des INTERLIS-Elements                        |
| attributePath    | String                             | Pfad zur betroffenen Eigenschaft (z.B. Attributname); ist bei valueType reference, attribute, und geometry vorhanden und null bei valueType object) |
| oldValue         | WKT String &#124; Array &#124; Object               | Alter Wert der Eigenschaft (null bei "added")                               |
| newValue         | WKT String &#124; Array &#124; Object               | Neuer Wert der Eigenschaft (null bei "deleted")                             |

### Beispiele

#### Neuer Attributwert
```json
[
  {
    "oid": "a51b05ff-a561-43dd-a1a6-cba699d6aa67",
    "changeType": "added",
    "valueType": "attribute",
    "interlisName": "DMAV_Bodenbedeckung_V1_0.Bodenbedeckung.Messpunkt",
    "attributePath": "Hoehengenauigkeit",
    "oldValue": null,
    "newValue": "0.040"
  }
]
```

#### Gelöschtes Objekt
```json
[
    {
        "oid": "30f04ceb-44fd-49cb-8326-f9c1ceed8edd",
        "changeType": "deleted",
        "valueType": "reference",
        "interlisName": "DMAV_Grundstuecke_V1_0.Grundstuecke.GSNachfuehrung",
        "attributePath": "entstehender_Grenzpunkt",
        "oldValue": [
            "9afc0fd7-af41-4643-ac7a-8247a87d2650"
        ],
        "newValue": null
    },
    {
        "oid": "9afc0fd7-af41-4643-ac7a-8247a87d2650",
        "changeType": "deleted",
        "valueType": "object",
        "interlisName": "DMAV_Grundstuecke_V1_0.Grundstuecke.Grenzpunkt",
        "attributePath": null,
        "oldValue": null,
        "newValue": null
    }
]
```
Da die Objektlöschung in diesem Fall auch eine Auswirkung auf die Referenzierten Objekte in der GSNachfuehrungstabelle hat, wird die Veränderung der Rererenz ebenfalls aufgelistet.

#### Objektveränderung
```json
[
    {
        "oid": "e9cf8d88-3898-4e2a-869c-32d45e3f21a2",
        "changeType": "changed",
        "valueType": "attribute",
        "interlisName": "DMAV_Grundstuecke_V1_0.Grundstuecke.Grundstueck",
        "attributePath": "Nummer",
        "oldValue": "1132",
        "newValue": "320"
    }
]
```



