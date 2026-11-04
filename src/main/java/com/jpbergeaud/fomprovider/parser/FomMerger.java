package com.jpbergeaud.fomprovider.parser;

import com.jpbergeaud.fomprovider.model.AttributeInfo;
import com.jpbergeaud.fomprovider.model.FomModel;
import com.jpbergeaud.fomprovider.model.InteractionClassNode;
import com.jpbergeaud.fomprovider.model.ObjectClassNode;
import com.jpbergeaud.fomprovider.model.ParameterInfo;

import java.util.Map;

/**
 * Fusionne le contenu d'un {@link FomModel} issu d'un fichier nouvellement chargé dans un modèle
 * cible déjà existant (l'arborescence affichée à l'écran).
 *
 * <p>La fusion se fait par nom de classe à chaque niveau de la hiérarchie : deux classes portant
 * le même nom au même endroit de l'arbre (par exemple deux définitions de {@code Aircraft} sous
 * {@code Platform} venant de deux fichiers différents) sont combinées en une seule, sans qu'il
 * soit possible de savoir ensuite quel attribut ou quelle classe provient de quel fichier —
 * conformément au principe d'une simple fusion, sans notion de référence vers les fichiers
 * sources.</p>
 */
public final class FomMerger {

    private FomMerger() {
    }

    public static void mergeInto(FomModel target, FomModel source) {
        mergeObjectClasses(target.getObjectClasses(), source.getObjectClasses());
        mergeInteractionClasses(target.getInteractionClasses(), source.getInteractionClasses());
    }

    private static void mergeObjectClasses(Map<String, ObjectClassNode> target,
                                            Map<String, ObjectClassNode> source) {
        for (Map.Entry<String, ObjectClassNode> entry : source.entrySet()) {
            ObjectClassNode incoming = entry.getValue();
            ObjectClassNode existing = target.get(entry.getKey());
            if (existing == null) {
                target.put(entry.getKey(), incoming);
                continue;
            }
            existing.mergeFrom(incoming);
            mergeAttributes(existing.getAttributes(), incoming.getAttributes());
            mergeObjectClasses(existing.getChildren(), incoming.getChildren());
        }
    }

    private static void mergeAttributes(Map<String, AttributeInfo> target,
                                         Map<String, AttributeInfo> source) {
        for (Map.Entry<String, AttributeInfo> entry : source.entrySet()) {
            AttributeInfo incoming = entry.getValue();
            AttributeInfo existing = target.get(entry.getKey());
            if (existing == null) {
                target.put(entry.getKey(), incoming);
            } else {
                existing.mergeFrom(incoming);
            }
        }
    }

    private static void mergeInteractionClasses(Map<String, InteractionClassNode> target,
                                                 Map<String, InteractionClassNode> source) {
        for (Map.Entry<String, InteractionClassNode> entry : source.entrySet()) {
            InteractionClassNode incoming = entry.getValue();
            InteractionClassNode existing = target.get(entry.getKey());
            if (existing == null) {
                target.put(entry.getKey(), incoming);
                continue;
            }
            existing.mergeFrom(incoming);
            mergeParameters(existing.getParameters(), incoming.getParameters());
            mergeInteractionClasses(existing.getChildren(), incoming.getChildren());
        }
    }

    private static void mergeParameters(Map<String, ParameterInfo> target,
                                         Map<String, ParameterInfo> source) {
        for (Map.Entry<String, ParameterInfo> entry : source.entrySet()) {
            ParameterInfo incoming = entry.getValue();
            ParameterInfo existing = target.get(entry.getKey());
            if (existing == null) {
                target.put(entry.getKey(), incoming);
            } else {
                existing.mergeFrom(incoming);
            }
        }
    }
}
