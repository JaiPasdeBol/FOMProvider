package com.jpbergeaud.fomprovider.ui;

import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Icône de l'application (fichier {@code icon.png} placé dans {@code src/main/resources}, donc à
 * la racine du classpath une fois le projet compilé, actuellement une image carrée 596x596).
 * Chargée une seule fois au premier accès ; si la ressource est absente, les méthodes renvoient
 * {@code null} (ou une liste vide) plutôt que d'échouer, afin que l'absence d'icône ne rende
 * jamais l'IHM inutilisable.
 *
 * <p>Le redimensionnement se fait par réductions successives par paliers de moitié (plutôt qu'en
 * une seule passe) avec interpolation bicubique : {@link Image#getScaledInstance} donne un rendu
 * flou/pixelisé sur de forts facteurs de réduction (596 vers 16 ou 32px, par exemple), alors que
 * cette réduction progressive reste nette.</p>
 */
public final class AppIcon {

    private static final String RESOURCE_PATH = "/icon.png";

    /** Tailles fournies à la fenêtre : le système choisit celle la plus proche de son besoin
     *  (barre des tâches, barre de titre, alt-tab...) plutôt que de redimensionner lui-même une
     *  unique grande image. */
    private static final int[] WINDOW_ICON_SIZES = {16, 24, 32, 48, 64, 128, 256};

    private static final BufferedImage SOURCE = load();

    private AppIcon() {
    }

    private static BufferedImage load() {
        URL url = AppIcon.class.getResource(RESOURCE_PATH);
        if (url == null) {
            return null;
        }
        Image image = new ImageIcon(url).getImage();
        return toBufferedImage(image);
    }

    /** Image brute de l'icône, à sa résolution d'origine, ou {@code null} si la ressource est
     *  absente. */
    public static Image image() {
        return SOURCE;
    }

    /**
     * Plusieurs tailles de la même icône, pour {@link java.awt.Window#setIconImages}. Liste vide
     * si la ressource est absente.
     */
    public static List<Image> windowIcons() {
        List<Image> icons = new ArrayList<>();
        if (SOURCE == null) {
            return icons;
        }
        for (int size : WINDOW_ICON_SIZES) {
            icons.add(highQualityScale(SOURCE, size, size));
        }
        return icons;
    }

    /** Icône carrée redimensionnée au côté donné avec un rendu net (voir la classe), ou
     *  {@code null} si la ressource est absente. */
    public static ImageIcon scaled(int size) {
        if (SOURCE == null) {
            return null;
        }
        return new ImageIcon(highQualityScale(SOURCE, size, size));
    }

    private static BufferedImage highQualityScale(BufferedImage source, int targetWidth, int targetHeight) {
        BufferedImage current = source;
        int currentWidth = source.getWidth();
        int currentHeight = source.getHeight();
        while (currentWidth / 2 > targetWidth && currentHeight / 2 > targetHeight) {
            currentWidth = Math.max(targetWidth, currentWidth / 2);
            currentHeight = Math.max(targetHeight, currentHeight / 2);
            current = drawScaled(current, currentWidth, currentHeight);
        }
        return drawScaled(current, targetWidth, targetHeight);
    }

    private static BufferedImage drawScaled(BufferedImage source, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(source, 0, 0, width, height, null);
        g2.dispose();
        return result;
    }

    private static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage buffered) {
            return buffered;
        }
        BufferedImage result = new BufferedImage(
                image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return result;
    }
}
