package service.Objectifs;

import Model.Objectif;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class VisionBoardService {

    // ── Dimensions ────────────────────────────────────────────────────────
    private static final int BOARD_W  = 1200;
    private static final int CARD_W   = 320;
    private static final int CARD_H   = 380;
    private static final int IMG_H    = 180;
    private static final int COLS     = 3;
    private static final int PADDING  = 50;
    private static final int GAP      = 28;

    // ── Palette scrapbook crème ───────────────────────────────────────────
    private static final Color BG_CREAM   = new Color(0xF5, 0xF0, 0xE8);
    private static final Color BG_WARM    = new Color(0xEE, 0xE8, 0xD8);
    private static final Color CARD_WHITE = new Color(0xFF, 0xFF, 0xFC);
    private static final Color CARD_CREAM = new Color(0xFD, 0xF8, 0xF0);
    private static final Color TEXT_DARK  = new Color(0x2C, 0x2C, 0x2C);
    private static final Color TEXT_MID   = new Color(0x6B, 0x6B, 0x6B);
    private static final Color TEXT_SOFT  = new Color(0xA0, 0x9A, 0x90);
    private static final Color ACCENT_GOLD = new Color(0xC8, 0xA9, 0x6E);
    private static final Color SHADOW     = new Color(0, 0, 0, 35);

    // Cache images Unsplash
    private final Map<String, BufferedImage> imageCache = new HashMap<>();
    private final Random rnd = new Random(42);

    // ── Mots-clés Unsplash par catégorie (kept for reference) ────────────
    private BufferedImage telechargerImage(String categorie) {
        String key = categorie != null ? categorie.toLowerCase() : "default";
        if (imageCache.containsKey(key)) return imageCache.get(key);

        // Essayer plusieurs APIs gratuites
        String[] urls = {
            getPicsumsUrl(key),           // picsum.photos — toujours disponible
            getLorempicsumUrl(key),        // alternative
        };

        for (String url : urls) {
            try {
                HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 LifeOps/1.0")
                    .GET()
                    .build();

                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() == 200 && response.body().length > 1000) {
                    java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(response.body());
                    BufferedImage img = ImageIO.read(bis);
                    if (img != null) {
                        BufferedImage resized = new BufferedImage(CARD_W, IMG_H, BufferedImage.TYPE_INT_RGB);
                        Graphics2D gr = resized.createGraphics();
                        gr.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        gr.drawImage(img, 0, 0, CARD_W, IMG_H, null);
                        gr.dispose();
                        imageCache.put(key, resized);
                        return resized;
                    }
                }
            } catch (Exception e) {
                System.err.println("Image API failed (" + url + "): " + e.getMessage());
            }
        }

        return creerImageFallback(categorie);
    }

    // Picsum Photos — images aléatoires belles, toujours disponible, seed par catégorie
    private String getPicsumsUrl(String categorie) {
        int seed = switch (categorie != null ? categorie.toLowerCase() : "") {
            case "santé", "sante" -> 10;
            case "finances"       -> 20;
            case "etudes"         -> 30;
            case "loisirs"        -> 40;
            case "personnel"      -> 50;
            default               -> 60;
        };
        return "https://picsum.photos/seed/" + seed + "/" + CARD_W + "/" + IMG_H;
    }

    private String getLorempicsumUrl(String categorie) {
        int id = switch (categorie != null ? categorie.toLowerCase() : "") {
            case "santé", "sante" -> 15;
            case "finances"       -> 25;
            case "etudes"         -> 35;
            case "loisirs"        -> 45;
            case "personnel"      -> 55;
            default               -> 65;
        };
        return "https://picsum.photos/id/" + id + "/" + CARD_W + "/" + IMG_H;
    }

    private BufferedImage creerImageFallback(String categorie) {
        BufferedImage img = new BufferedImage(CARD_W, IMG_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Color c1 = getCouleurAccent(categorie);
        GradientPaint gp = new GradientPaint(0, 0, c1, CARD_W, IMG_H, c1.brighter());
        g.setPaint(gp);
        g.fillRect(0, 0, CARD_W, IMG_H);
        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
        g.setColor(new Color(255, 255, 255, 180));
        String emoji = getEmojiCategorie(categorie);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(emoji, (CARD_W - fm.stringWidth(emoji)) / 2, IMG_H / 2 + 20);
        g.dispose();
        return img;
    }

    // ── Export principal ──────────────────────────────────────────────────

    public void exporter(List<Objectif> objectifs, File fichier) throws IOException {
        if (objectifs == null || objectifs.isEmpty())
            throw new IOException("Aucun objectif à exporter.");

        int rows   = (int) Math.ceil((double) objectifs.size() / COLS);
        int cardsH = rows * CARD_H + (rows + 1) * GAP;
        int height = PADDING + 270 + cardsH + 120 + PADDING;

        BufferedImage board = new BufferedImage(BOARD_W, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = board.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        dessinerFond(g, BOARD_W, height);
        dessinerDecorations(g, BOARD_W, height);
        dessinerTitre(g, BOARD_W, PADDING);

        int startY = PADDING + 260;
        for (int i = 0; i < objectifs.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int x   = PADDING + GAP + col * (CARD_W + GAP);
            int y   = startY + GAP + row * (CARD_H + GAP);
            double angle = (rnd.nextDouble() - 0.5) * 0.06;
            dessinerCarteScrapbook(g, objectifs.get(i), x, y, i, angle);
        }

        dessinerFooter(g, BOARD_W, height);
        g.dispose();

        String fmt = fichier.getName().toLowerCase().endsWith(".jpg") ? "jpg" : "png";
        ImageIO.write(board, fmt, fichier);
    }

    // ── Fond crème texturé ────────────────────────────────────────────────

    private void dessinerFond(Graphics2D g, int w, int h) {
        GradientPaint bg = new GradientPaint(0, 0, BG_CREAM, w, h, BG_WARM);
        g.setPaint(bg);
        g.fillRect(0, 0, w, h);

        // Grain subtil
        Random r = new Random(7);
        for (int i = 0; i < 8000; i++) {
            g.setColor(new Color(180, 160, 130, 8 + r.nextInt(12)));
            g.fillRect(r.nextInt(w), r.nextInt(h), 1, 1);
        }

        // Bordure dorée
        g.setColor(new Color(0xC8, 0xA9, 0x6E, 60));
        g.setStroke(new BasicStroke(3f));
        g.drawRect(20, 20, w - 40, h - 40);
        g.setColor(new Color(0xC8, 0xA9, 0x6E, 30));
        g.setStroke(new BasicStroke(1f));
        g.drawRect(26, 26, w - 52, h - 52);
        g.setStroke(new BasicStroke(1f));
    }

    // ── Décorations botaniques ────────────────────────────────────────────

    private void dessinerDecorations(Graphics2D g, int w, int h) {
        dessinerFeuilles(g, 35, 35, 1.0, 0);
        dessinerFeuilles(g, w - 35, 35, -1.0, 0);
        dessinerFeuilles(g, 35, h - 35, 1.0, Math.PI);
        dessinerFeuilles(g, w - 35, h - 35, -1.0, Math.PI);

        // Points dorés décoratifs
        g.setColor(new Color(0xC8, 0xA9, 0x6E, 80));
        Random r = new Random(13);
        for (int i = 0; i < 30; i++) {
            int sz = 2 + r.nextInt(4);
            g.fillOval(r.nextInt(w), r.nextInt(h), sz, sz);
        }
    }

    private void dessinerFeuilles(Graphics2D g, int x, int y, double scaleX, double rotation) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        g2.rotate(rotation);
        g2.scale(scaleX, 1.0);

        Color leafColor = new Color(0x7A, 0x9E, 0x7E, 120);
        Color stemColor = new Color(0x5A, 0x7E, 0x5E, 100);

        g2.setColor(stemColor);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(0, 0, 80, 80);

        int[][] leaves = {{20, 20}, {40, 40}, {60, 60}};
        for (int[] pos : leaves) {
            g2.setColor(leafColor);
            GeneralPath leaf = new GeneralPath();
            leaf.moveTo(pos[0], pos[1]);
            leaf.curveTo(pos[0] - 20, pos[1] - 30, pos[0] - 40, pos[1] - 10, pos[0] - 25, pos[1] + 5);
            leaf.curveTo(pos[0] - 15, pos[1] + 15, pos[0] - 5, pos[1] + 5, pos[0], pos[1]);
            g2.fill(leaf);
        }
        g2.dispose();
    }

    // ── Titre central ─────────────────────────────────────────────────────

    private void dessinerTitre(Graphics2D g, int w, int y) {
        // "my" italique
        g.setFont(new Font("Georgia", Font.ITALIC, 52));
        g.setColor(new Color(0x8B, 0x7D, 0x6B));
        String my = "my";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(my, (w - fm.stringWidth(my)) / 2, y + 70);

        // "VISION BOARD" bold
        g.setFont(new Font("Georgia", Font.BOLD, 90));
        g.setColor(TEXT_DARK);
        String vb = "VISION BOARD";
        fm = g.getFontMetrics();
        // Ombre
        g.setColor(new Color(0, 0, 0, 20));
        g.drawString(vb, (w - fm.stringWidth(vb)) / 2 + 3, y + 168);
        g.setColor(TEXT_DARK);
        g.drawString(vb, (w - fm.stringWidth(vb)) / 2, y + 165);

        // Ligne dorée
        g.setColor(new Color(0xC8, 0xA9, 0x6E, 150));
        g.setStroke(new BasicStroke(1.5f));
        int lw = 300;
        g.drawLine((w - lw) / 2, y + 182, (w + lw) / 2, y + 182);
        g.setStroke(new BasicStroke(1f));

        // Cœurs
        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        g.setColor(new Color(0xC8, 0xA9, 0x6E, 180));
        g.drawString("♡", (w - lw) / 2 - 25, y + 185);
        g.drawString("♡", (w + lw) / 2 + 10, y + 185);

        // Sous-titre
        g.setFont(new Font("Georgia", Font.PLAIN, 16));
        g.setColor(TEXT_MID);
        String sub = "DREAM BIG  •  PLAN WELL  •  TAKE ACTION  •  MAKE IT HAPPEN";
        fm = g.getFontMetrics();
        g.drawString(sub, (w - fm.stringWidth(sub)) / 2, y + 212);

        // Date
        g.setFont(new Font("Georgia", Font.ITALIC, 13));
        g.setColor(TEXT_SOFT);
        String date = LocalDate.now().toString();
        fm = g.getFontMetrics();
        g.drawString(date, (w - fm.stringWidth(date)) / 2, y + 235);
    }

    // ── Carte scrapbook ───────────────────────────────────────────────────

    private void dessinerCarteScrapbook(Graphics2D g, Objectif obj, int x, int y, int index, double angle) {
        AffineTransform original = g.getTransform();
        g.rotate(angle, x + CARD_W / 2.0, y + CARD_H / 2.0);

        // Ombre
        g.setColor(SHADOW);
        g.fill(new RoundRectangle2D.Float(x + 6, y + 8, CARD_W, CARD_H, 4, 4));

        // Fond carte
        g.setColor(index % 2 == 0 ? CARD_WHITE : CARD_CREAM);
        g.fill(new RoundRectangle2D.Float(x, y, CARD_W, CARD_H, 4, 4));

        // Photo Unsplash
        BufferedImage photo = telechargerImage(obj.getCategorie());
        if (photo != null) {
            Shape clip = new RoundRectangle2D.Float(x, y, CARD_W, IMG_H, 4, 4);
            g.setClip(clip);
            g.drawImage(photo, x, y, CARD_W, IMG_H, null);
            g.setClip(null);

            // Overlay dégradé bas de l'image
            GradientPaint overlay = new GradientPaint(
                x, y + IMG_H - 40, new Color(0, 0, 0, 0),
                x, y + IMG_H, new Color(0, 0, 0, 60)
            );
            g.setPaint(overlay);
            g.fill(new Rectangle(x, y + IMG_H - 40, CARD_W, 40));
        }

        // Contenu texte
        int tx = x + 16;
        int ty = y + IMG_H + 22;
        Color accent = getCouleurAccent(obj.getCategorie());

        // Catégorie
        g.setFont(new Font("Georgia", Font.BOLD, 13));
        g.setColor(accent);
        g.drawString(obj.getCategorie() != null ? obj.getCategorie().toUpperCase() : "OBJECTIF", tx, ty);

        // Titre
        g.setFont(new Font("Georgia", Font.BOLD, 16));
        g.setColor(TEXT_DARK);
        g.drawString(tronquer(obj.getTitre(), 32), tx, ty + 22);

        // Description
        if (obj.getDescription() != null && !obj.getDescription().isBlank()) {
            g.setFont(new Font("Georgia", Font.ITALIC, 12));
            g.setColor(TEXT_MID);
            String desc = obj.getDescription();
            g.drawString(tronquer(desc, 40), tx, ty + 42);
            if (desc.length() > 40)
                g.drawString(tronquer(desc.substring(40), 40), tx, ty + 56);
        }

        // Barre de progression
        int barY = y + CARD_H - 65;
        int barW = CARD_W - 32;
        int barH = 5;

        g.setColor(new Color(0xE0, 0xD8, 0xCC));
        g.fill(new RoundRectangle2D.Float(tx, barY, barW, barH, barH, barH));

        int fill = Math.max(0, (int) (barW * obj.getProgression() / 100.0));
        if (fill > 0) {
            g.setColor(accent);
            g.fill(new RoundRectangle2D.Float(tx, barY, fill, barH, barH, barH));
        }

        g.setFont(new Font("Georgia", Font.BOLD, 12));
        g.setColor(accent);
        g.drawString(obj.getProgression() + "%", tx + barW - 28, barY - 3);

        // Statut + date fin
        g.setFont(new Font("Georgia", Font.PLAIN, 11));
        g.setColor(TEXT_SOFT);
        g.drawString(obj.getStatut() != null ? obj.getStatut() : "—", tx, y + CARD_H - 18);

        if (obj.getDate_fin() != null) {
            String dateFin = "→ " + obj.getDate_fin();
            g.setFont(new Font("Georgia", Font.ITALIC, 11));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(dateFin, x + CARD_W - fm.stringWidth(dateFin) - 16, y + CARD_H - 18);
        }

        // Cœur décoratif
        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        g.setColor(new Color(0xC8, 0xA9, 0x6E, 200));
        g.drawString("♡", x + CARD_W - 28, ty + 2);

        // Bordure fine
        g.setColor(new Color(0xD0, 0xC8, 0xB8, 120));
        g.setStroke(new BasicStroke(0.8f));
        g.draw(new RoundRectangle2D.Float(x, y, CARD_W, CARD_H, 4, 4));
        g.setStroke(new BasicStroke(1f));

        g.setTransform(original);
    }

    // ── Footer ────────────────────────────────────────────────────────────

    private void dessinerFooter(Graphics2D g, int w, int h) {
        int fy = h - 100;

        g.setColor(new Color(0xC8, 0xA9, 0x6E, 100));
        g.setStroke(new BasicStroke(1f));
        g.drawLine(PADDING + 50, fy, w - PADDING - 50, fy);
        g.setStroke(new BasicStroke(1f));

        g.setFont(new Font("Georgia", Font.BOLD, 18));
        g.setColor(TEXT_DARK);
        String quote = "THE LIFE OF MY DREAMS IS ALREADY IN THE MAKING.";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(quote, (w - fm.stringWidth(quote)) / 2, fy + 30);

        g.setFont(new Font("Georgia", Font.ITALIC, 15));
        g.setColor(TEXT_MID);
        String sub = "I'm just getting started.  ♡";
        fm = g.getFontMetrics();
        g.drawString(sub, (w - fm.stringWidth(sub)) / 2, fy + 55);

        g.setFont(new Font("Georgia", Font.PLAIN, 11));
        g.setColor(TEXT_SOFT);
        String gen = "Généré par LifeOps  •  " + LocalDate.now();
        fm = g.getFontMetrics();
        g.drawString(gen, (w - fm.stringWidth(gen)) / 2, fy + 78);
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

    private Color getCouleurAccent(String categorie) {
        if (categorie == null) return new Color(0x8B, 0x7D, 0x6B);
        return switch (categorie.toLowerCase()) {
            case "santé", "sante" -> new Color(0xC0, 0x6B, 0x6B);
            case "finances"       -> new Color(0x6B, 0x9E, 0x6B);
            case "etudes"         -> new Color(0x6B, 0x8B, 0xC0);
            case "loisirs"        -> new Color(0xC0, 0x9E, 0x6B);
            case "personnel"      -> new Color(0x9E, 0x6B, 0xC0);
            default               -> new Color(0x8B, 0x7D, 0x6B);
        };
    }
}
