package com.jpbergeaud.fomprovider.ui;

import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * Illustration décorative (le personnage de {@code icon.png}) utilisée dans les panneaux de l'IHM,
 * par exemple le panneau de détail lorsqu'aucun élément n'est sélectionné.
 *
 * <p>Volontairement indépendante de {@link AppIcon} : cette dernière fournit uniquement les tailles
 * destinées à l'icône système de la fenêtre (barre des tâches, barre de titre...). Ici, l'image est
 * rechargée directement depuis les ressources du projet et redimensionnée à la demande.</p>
 *
 * <p>L'image renvoyée est une {@link BaseMultiResolutionImage} regroupant plusieurs variantes
 * (x1, x1.25, x1.5, x1.75, x2, x2.5, x3) de la taille demandée : sous Windows, lorsque l'affichage
 * est configuré avec une mise à l'échelle supérieure à 100% (125%, 150%, 200%...), Swing dessine
 * les icônes à une résolution physique plus élevée que la taille logique demandée. Une simple
 * image bitmap unique s'y retrouve alors étirée par le système et apparaît floue/pixelisée, quelle
 * que soit la qualité du redimensionnement d'origine — c'est le même phénomène que celui déjà
 * rencontré avec la taille des polices. Fournir plusieurs résolutions permet à Swing de choisir
 * automatiquement la variante adaptée à l'échelle réelle de l'écran.</p>
 */
public final class MascotIcon {

    private static final String RESOURCE_PATH = "/icon.png";

    /** Facteurs d'échelle couvrant les paliers de mise à l'échelle Windows courants (100% à 300%). */
    private static final double[] RESOLUTION_FACTORS = {1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0};

    private static final BufferedImage SOURCE = load();

    private MascotIcon() {
    }

    private static BufferedImage load() {
        URL url = MascotIcon.class.getResource(RESOURCE_PATH);
        if (url == null) {
            return null;
        }
        Image image = new ImageIcon(url).getImage();
        return toBufferedImage(image);
    }

    /** Illustration carrée redimensionnée au côté donné avec un rendu net (interpolation bicubique,
     *  réduction progressive par paliers de moitié) et adaptée aux écrans à mise à l'échelle Windows
     *  (voir la classe), ou {@code null} si la ressource est absente. */
    public static ImageIcon scaled(int size) {
        if (SOURCE == null) {
            return null;
        }
        Image[] variants = new Image[RESOLUTION_FACTORS.length];
        for (int i = 0; i < RESOLUTION_FACTORS.length; i++) {
            int variantSize = Math.max(1, (int) Math.round(size * RESOLUTION_FACTORS[i]));
            variants[i] = highQualityScale(SOURCE, variantSize, variantSize);
        }
        return new ImageIcon(new BaseMultiResolutionImage(variants));
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
