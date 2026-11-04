package com.jpbergeaud.fomprovider.parser;

/**
 * Signale qu'un fichier FOM n'a pas pu être lu ou ne correspond pas au format attendu
 * (élément racine {@code objectModel} absent, XML invalide, etc.).
 */
public class FomParseException extends Exception {

    public FomParseException(String message) {
        super(message);
    }

    public FomParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
