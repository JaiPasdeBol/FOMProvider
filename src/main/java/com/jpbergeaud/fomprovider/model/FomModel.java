package com.jpbergeaud.fomprovider.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Modèle objet fédératif (FOM) en mémoire : ensemble des classes d'objets et d'interactions
 * racines. Utilisé aussi bien comme résultat brut du parsing d'un fichier unique que comme modèle
 * fusionné accumulant plusieurs fichiers chargés successivement.
 */
public class FomModel {

    /** Classes d'objets de premier niveau (typiquement HLAobjectRoot), indexées par nom. */
    private final Map<String, ObjectClassNode> objectClasses = new LinkedHashMap<>();

    /** Classes d'interactions de premier niveau (typiquement HLAinteractionRoot), indexées par nom. */
    private final Map<String, InteractionClassNode> interactionClasses = new LinkedHashMap<>();

    public Map<String, ObjectClassNode> getObjectClasses() {
        return objectClasses;
    }

    public Map<String, InteractionClassNode> getInteractionClasses() {
        return interactionClasses;
    }

    public void clear() {
        objectClasses.clear();
        interactionClasses.clear();
    }
}
