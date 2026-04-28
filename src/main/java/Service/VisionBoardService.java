package Service;

import Model.Objectif;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class VisionBoardService {

    // ── Dimensions ────────────────────────────────────────────────────────
    private static final int COLS     = 3;
    private static final int CARD_W   = 360;
    private static final int CARD_H   = 240;
    private static final int PADDING  = 40;
    private static final int HEADER_H = 130;
    private static final int FOOTER_H = 60;
    private static final int GAP      = 24;

    // ── Palette joyeuse ───────────────────────────────────────────────────
    private static final Color BG_TOP    = new Color(0xFF, 0xF0, 0xF5); // rose très clair
    private static final Color BG_BOT    = new Color(0xF0, 0xF4, 0xFF); // bleu très clair
    private static final Color TEXT_DARK = new Color(0x2D, 0x2D, 0x3A);
    private static final Color TEXT_MID  = new Color(0x6B, 0x6B, 0x80);
    private static final Color TEXT_SOFT = new Color(0xA0, 0xA0, 0xB8);
    private static final Color WHITE     = Color.WHITE;

    // Couleurs vives par catégorie (fond carte)
    private static final Color[] CARD_GRADIENTS_TOP = {
        new Color(0xFF, 0xD6, 0xE7), // rose
        new Color(0xD6, 0xF0, 0xFF), // bleu ciel
        new Color(0xD6, 0xFF, 0xE8), // vert menthe
        new Color(0xFF, 0xF0, 0xD6), // orange doux
        new Color(0xE8, 0xD6, 0xFF), // violet doux
        new Color(0xFF, 0xFF, 0xD6), // jaune doux
    };

    public void exporter(List<Objectif> objectifs, File fichier) throws IOException {
        if (objectifs == null || objectifs.isEmpty())
            throw new IOException("Aucun objectif à exporter.");

        int rows   = (int) Math.ceil((double) objectifs.size() / COLS);
        int width  = COLS * CARD_W + (COLS + 1) * GAP + 2 * PADDING;
        int height = HEADER_H + rows * CARD_H + (rows + 1) * GAP + FOOTER_H + 2 * PADDING;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        // ── Fond dégradé pastel ───────────────────────────────────────────
        GradientPaint bgGrad = new GradientPaint(0, 0, BG_TOP, 0, height, BG_BOT);
        g.setPaint(bgGrad);
        g.fillRect(0, 0, width, height);

        // ── Confettis décoratifs ──────────────────────────────────────────
        dessinerConfettis(g, width, height);

        // ── Header ────────────────────────────────────────────────────────
        dessinerHeader(g, width, objectifs.size());

        // ── Cartes ────────────────────────────────────────────────────────
        for (int i = 0; i < objectifs.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int x   = PADDING + GAP + col * (CARD_W + GAP);
            int y   = PADDING + HEADER_H + GAP + row * (CARD_H + GAP);
            dessinerCarte(g, objectifs.get(i), x, y, i);
        }

        // ── Footer ────────────────────────────────────────────────────────
        dessinerFooter(g, width, height);

        g.dispose();

        String fmt = fichier.getName().toLowerCase().endsWith(".jpg") ? "jpg" : "png";
        ImageIO.write(image, fmt, fichier);
    }

    // ── Confettis ─────────────────────────────────────────────────────────

    private void dessinerConfettis(Graphics2D g, int width, int height) {
        Color[] confettiColors = {
            new Color(0xFF, 0x6B, 0x9D, 80),
            new Color(0x6B, 0xC5, 0xFF, 80),
            new Color(0x6B, 0xFF, 0xB4, 80),
            new Color(0xFF, 0xD9, 0x6B, 80),
            new Color(0xC5, 0x6B, 0xFF, 80),
        };
        java.util.Random rnd = new java.util.Random(42); // seed fixe = même résultat à chaque export
        for (int i = 0; i < 60; i++) {
            g.setColor(confettiColors[i % confettiColors.length]);
            int cx = rnd.nextInt(width);
            int cy = rnd.nextInt(height);
            int sz = 6 + rnd.nextInt(10);
            int shape = rnd.nextInt(3);
            if (shape == 0) g.fillOval(cx, cy, sz, sz / 2);
            else if (shape == 1) g.fillRect(cx, cy, sz / 2, sz);
            else g.fillRoundRect(cx, cy, sz, sz, 4, 4);
        }
    }

    // ── Header ────────────────────────────────────────────────────────────

    private void dessinerHeader(Graphics2D g, int width, int nbObjectifs) {
        // Carte blanche arrondie pour le header
        g.setColor(WHITE);
        g.fill(new RoundRectangle2D.Float(PADDING, PADDING, width - 2 * PADDING, HEADER_H - 10, 24, 24));

        // Ombre légère
        g.setColor(new Color(0, 0, 0, 15));
        g.fill(new RoundRectangle2D.Float(PADDING + 3, PADDING + 4, width - 2 * PADDING, HEADER_H - 10, 24, 24));
        g.setColor(WHITE);
        g.fill(new RoundRectangle2D.Float(PADDING, PADDING, width - 2 * PADDING, HEADER_H - 10, 24, 24));

        // Titre avec emoji
        Font emojiFont = new Font("Segoe UI Emoji", Font.PLAIN, 36);
        g.setFont(emojiFont);
        g.setColor(TEXT_DARK);
        String emoji = "🌟";
        g.drawString(emoji, PADDING + 30, PADDING + 55);

        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.setColor(TEXT_DARK);
        g.drawString("Mon Vision Board", PADDING + 80, PADDING + 52);

        // Sous-titre coloré
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.setColor(TEXT_MID);
        String sub = "✨  " + nbObjectifs + " objectif" + (nbObjectifs > 1 ? "s" : "") +
                     "  •  " + LocalDate.now() + "  •  Crois en toi ! 💪";
        g.drawString(sub, PADDING + 30, PADDING + 80);

        // Petite ligne décorative arc-en-ciel
        int lx = PADDING + 30;
        int ly = PADDING + 92;
        int lw = (width - 2 * PADDING - 60) / 5;
        Color[] rainbow = {
            new Color(0xFF, 0x6B, 0x9D),
            new Color(0xFF, 0xA5, 0x6B),
            new Color(0xFF, 0xD9, 0x6B),
            new Color(0x6B, 0xFF, 0xB4),
            new Color(0x6B, 0xC5, 0xFF),
        };
        for (int i = 0; i < 5; i++) {
            g.setColor(rainbow[i]);
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(lx + i * lw, ly, lx + (i + 1) * lw - 4, ly);
        }
        g.setStroke(new BasicStroke(1f));
    }

    // ── Carte objectif ────────────────────────────────────────────────────

    private void dessinerCarte(Graphics2D g, Objectif obj, int x, int y, int index) {
        Color cardColor = CARD_GRADIENTS_TOP[index % CARD_GRADIENTS_TOP.length];
        Color accentColor = getCouleurAccent(obj.getCategorie());

        // Ombre de la carte
        g.setColor(new Color(0, 0, 0, 20));
        g.fill(new RoundRectangle2D.Float(x + 4, y + 5, CARD_W, CARD_H, 20, 20));

        // Fond carte dégradé pastel
        GradientPaint cardGrad = new GradientPaint(x, y, cardColor, x, y + CARD_H, WHITE);
        g.setPaint(cardGrad);
        g.fill(new RoundRectangle2D.Float(x, y, CARD_W, CARD_H, 20, 20));

        // Bordure colorée
        g.setColor(accentColor);
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new RoundRectangle2D.Float(x, y, CARD_W, CARD_H, 20, 20));
        g.setStroke(new BasicStroke(1f));

        // Bande colorée en haut
        g.setColor(accentColor);
        g.fill(new RoundRectangle2D.Float(x, y, CARD_W, 8, 8, 8));
        g.fillRect(x, y + 4, CARD_W, 4);

        int cx = x + 18;
        int cy = y + 30;

        // Grand emoji catégorie
        Font emojiFont = new Font("Segoe UI Emoji", Font.PLAIN, 28);
        g.setFont(emojiFont);
        g.drawString(getEmojiCategorie(obj.getCategorie()), cx, cy + 4);

        // Catégorie texte
        g.setFont(new Font("Arial", Font.BOLD, 11));
        g.setColor(accentColor);
        String cat = (obj.getCategorie() != null ? obj.getCategorie().toUpperCase() : "AUTRE");
        g.drawString(cat, cx + 38, cy - 6);

        // Titre
        g.setFont(new Font("Arial", Font.BOLD, 17));
        g.setColor(TEXT_DARK);
        g.drawString(tronquer(obj.getTitre(), 28), cx + 38, cy + 10);

        // Description
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.setColor(TEXT_MID);
        if (obj.getDescription() != null && !obj.getDescription().isBlank()) {
            g.drawString(tronquer(obj.getDescription(), 50), cx, cy + 32);
        }

        // ── Barre de progression ──────────────────────────────────────────
        int barY = y + CARD_H - 85;
        int barW = CARD_W - 36;
        int barH = 12;

        // Label progression
        g.setFont(new Font("Arial", Font.BOLD, 11));
        g.setColor(TEXT_MID);
        g.drawString("Progression", cx, barY - 4);

        // Emoji progression
        Font ef = new Font("Segoe UI Emoji", Font.PLAIN, 11);
        g.setFont(ef);
        String progEmoji = obj.getProgression() >= 100 ? "🏆" :
                           obj.getProgression() >= 75  ? "🔥" :
                           obj.getProgression() >= 50  ? "💪" :
                           obj.getProgression() >= 25  ? "🌱" : "🚀";
        g.drawString(progEmoji, cx + 80, barY - 3);

        // Fond barre
        g.setColor(new Color(0, 0, 0, 20));
        g.fill(new RoundRectangle2D.Float(cx, barY, barW, barH, barH, barH));

        // Remplissage barre dégradé
        int fill = Math.max(0, (int) (barW * obj.getProgression() / 100.0));
        if (fill > 0) {
            GradientPaint barGrad = new GradientPaint(cx, barY, accentColor,
                cx + fill, barY, accentColor.brighter());
            g.setPaint(barGrad);
            g.fill(new RoundRectangle2D.Float(cx, barY, fill, barH, barH, barH));
        }

        // Pourcentage
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(accentColor);
        g.drawString(obj.getProgression() + "%", cx + barW - 32, barY + barH - 1);

        // ── Statut badge ──────────────────────────────────────────────────
        int statY = y + CARD_H - 58;
        String statutEmoji = getEmojiStatut(obj.getStatut());
        dessinerBadge(g, cx, statY, statutEmoji + " " + (obj.getStatut() != null ? obj.getStatut() : "—"),
                      getCouleurStatut(obj.getStatut()));

        // ── Dates ─────────────────────────────────────────────────────────
        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 10));
        g.setColor(TEXT_SOFT);
        int dateY = y + CARD_H - 16;
        if (obj.getDate_debut() != null)
            g.drawString("📅 " + obj.getDate_debut(), cx, dateY);
        if (obj.getDate_fin() != null)
            g.drawString("🏁 " + obj.getDate_fin(), cx + 170, dateY);
    }

    // ── Badge ─────────────────────────────────────────────────────────────

    private void dessinerBadge(Graphics2D g, int x, int y, String texte, Color couleur) {
        if (texte == null) return;
        g.setFont(new Font("Segoe UI Emoji", Font.BOLD, 11));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(texte);
        int bw = tw + 18;
        int bh = 20;

        // Fond badge
        g.setColor(new Color(couleur.getRed(), couleur.getGreen(), couleur.getBlue(), 35));
        g.fill(new RoundRectangle2D.Float(x, y, bw, bh, bh, bh));

        // Bordure
        g.setColor(couleur);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Float(x, y, bw, bh, bh, bh));
        g.setStroke(new BasicStroke(1f));

        // Texte
        g.setColor(couleur.darker());
        g.drawString(texte, x + 9, y + bh - 5);
    }

    // ── Footer ────────────────────────────────────────────────────────────

    private void dessinerFooter(Graphics2D g, int width, int height) {
        // Carte blanche arrondie
        int fy = height - FOOTER_H - PADDING + 10;
        g.setColor(new Color(WHITE.getRed(), WHITE.getGreen(), WHITE.getBlue(), 180));
        g.fill(new RoundRectangle2D.Float(PADDING, fy, width - 2 * PADDING, FOOTER_H - 10, 16, 16));

        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        g.setColor(TEXT_MID);
        String footer = "🌈  Généré par LifeOps  •  " + LocalDate.now() + "  •  Keep going! 🚀✨";
        FontMetrics fm = g.getFontMetrics();
        int fx = (width - fm.stringWidth(footer)) / 2;
        g.drawString(footer, fx, fy + 28);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String tronquer(String texte, int max) {
        if (texte == null) return "";
        return texte.length() > max ? texte.substring(0, max - 1) + "…" : texte;
    }

    private String getEmojiCategorie(String categorie) {
        if (categorie == null) return "🎯";
        return switch (categorie.toLowerCase()) {
            case "santé", "sante" -> "💪";
            case "finances"       -> "💰";
            case "etudes"         -> "📚";
            case "loisirs"        -> "🎮";
            case "personnel"      -> "🌟";
            default               -> "🎯";
        };
    }

    private String getEmojiStatut(String statut) {
        if (statut == null) return "❓";
        return switch (statut.toLowerCase()) {
            case "complété", "complete", "terminé", "termine" -> "✅";
            case "en cours"                                    -> "⚡";
            case "abandonné", "abandonne"                      -> "❌";
            case "en pause"                                    -> "⏸";
            default                                            -> "📌";
        };
    }

    private Color getCouleurAccent(String categorie) {
        if (categorie == null) return new Color(0x8B, 0x5C, 0xF6);
        return switch (categorie.toLowerCase()) {
            case "santé", "sante" -> new Color(0xFF, 0x47, 0x7E);
            case "finances"       -> new Color(0x00, 0xC2, 0x7A);
            case "etudes"         -> new Color(0x3B, 0x82, 0xF6);
            case "loisirs"        -> new Color(0xF5, 0x9E, 0x0B);
            case "personnel"      -> new Color(0x8B, 0x5C, 0xF6);
            default               -> new Color(0x6B, 0x7B, 0x8D);
        };
    }

    private Color getCouleurStatut(String statut) {
        if (statut == null) return new Color(0x6B, 0x7B, 0x8D);
        return switch (statut.toLowerCase()) {
            case "complété", "complete", "terminé", "termine" -> new Color(0x00, 0xC2, 0x7A);
            case "en cours"                                    -> new Color(0x3B, 0x82, 0xF6);
            case "abandonné", "abandonne"                      -> new Color(0xFF, 0x47, 0x7E);
            case "en pause"                                    -> new Color(0xF5, 0x9E, 0x0B);
            default                                            -> new Color(0x6B, 0x7B, 0x8D);
        };
    }
}
