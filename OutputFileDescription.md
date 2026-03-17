# JSON-Dokumentation für XTF-Diff-Tool

Dieses Dokument beschreibt die Struktur und Bedeutung der Eigenschaften im JSON-Ausgabeformat des XTF-Diff-Tools. 

## Struktur des JSON-Objekts
Jedes Element im JSON-Array repräsentiert eine Änderung an einem Objekt und enthält folgende Eigenschaften:

| Eigenschaft      | Typ / Werte                        | Beschreibung                                                                 |
|------------------|------------------------------------|------------------------------------------------------------------------------|
| oid              | UUID                               | Transfer-ID des betroffenen Objekts                                          |
| changeType       | "changed" &#124; "added" &#124; "deleted" | Typ der Änderung (hinzugefügt, geändert, gelöscht)                           |
| valueType        | "object" &#124; "reference" &#124; "attribute" &#124; "geometry" | Typ der geänderten Objekteigenschaft                                         |
| interlisName     | String                             | INTERLIS Element Identifikator (Namespace und Klasse)                        |
| attributePath    | String                             | Pfad zur betroffenen Eigenschaft (z.B. Attributname, optional bei valueType) |
| oldValue         | String &#124; base64               | Alter Wert der Eigenschaft (null bei "added")                               |
| newValue         | String &#124; base64               | Neuer Wert der Eigenschaft (null bei "deleted")                             |

### Hinweise
- **oid**: Eindeutige ID des Objekts im Transfer.
- **changeType**: Gibt an, ob das Objekt oder die Eigenschaft hinzugefügt, geändert oder gelöscht wurde.
- **valueType**: Beschreibt, welche Art von Eigenschaft betroffen ist (z.B. Attribut, Referenz, Geometrie).
- **interlisName**: Vollqualifizierter Name des INTERLIS-Elements (inkl. Namespace).
- **attributePath**: Gibt den Pfad zur betroffenen Eigenschaft an, z.B. den Attributnamen.
- **oldValue/newValue**: Enthalten den alten bzw. neuen Wert. Werte können als String oder base64-codiert vorliegen, abhängig vom Datentyp.

#### Beispiel
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

1) **valueType**: Die Art der geänderten Eigenschaft (z.B. Attribut, Referenz, Geometrie).
2) **oldValue/newValue**: Werte können je nach Typ als String oder base64-codiert vorliegen.
