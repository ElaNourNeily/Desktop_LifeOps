package service.health;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import Model.health.BilanSante;
import Model.health.SuiviSante;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PdfExportService {

    private static final DeviceRgb VIOLET    = new DeviceRgb(83,  74,  183);
    private static final DeviceRgb VERT      = new DeviceRgb(46,  204, 113);
    private static final DeviceRgb ROUGE     = new DeviceRgb(231, 76,  60);
    private static final DeviceRgb GRIS_FOND = new DeviceRgb(241, 242, 246);
    private static final DeviceRgb GRIS_TXT  = new DeviceRgb(127, 140, 141);
    private static final DeviceRgb NOIR      = new DeviceRgb(44,  62,   80);

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    public void genererRapport(BilanSante bilan, List<SuiviSante> suivis, ChartImages chartImages, File outputFile) throws IOException {
        PdfWriter writer = new PdfWriter(outputFile);
        // Désactiver le flush immédiat pour permettre l'ajout des numéros de page à la fin
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4, false); // false = immediateFlush
        doc.setMargins(36, 40, 36, 40);

        ajouterCouverture(doc, bilan, suivis.size());
        doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        ajouterGraphiques(doc, chartImages);
        doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        ajouterTableauSuivis(doc, suivis);
        doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        ajouterRecommandations(doc, bilan);

        ajouterNumerosPages(pdf, doc);
        doc.close();
    }

    private void ajouterCouverture(Document doc, BilanSante bilan, int nbSuivis) {
        Table bandeau = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        Cell cellBandeau = new Cell().setBackgroundColor(VIOLET).setPadding(30).setBorder(null);

        cellBandeau.add(new Paragraph("RAPPORT DE BILAN SANTÉ IA").setFontColor(ColorConstants.WHITE).setBold().setFontSize(28).setTextAlignment(TextAlignment.CENTER));
        cellBandeau.add(new Paragraph(bilan.getDateDebut().format(dtf) + "  →  " + bilan.getDateFin().format(dtf)).setFontColor(new DeviceRgb(200, 196, 240)).setFontSize(13).setTextAlignment(TextAlignment.CENTER));

        bandeau.addCell(cellBandeau);
        doc.add(bandeau);

        doc.add(new Paragraph("\n"));
        doc.add(new Paragraph("RÉSUMÉ DU BILAN").setBold().setFontSize(16).setFontColor(VIOLET));
        doc.add(new Paragraph("Période : " + bilan.getDateDebut() + " au " + bilan.getDateFin()));
        doc.add(new Paragraph("Nombre de suivis analysés : " + nbSuivis));

        doc.add(new LineSeparator(new SolidLine()).setMarginTop(10).setMarginBottom(10));

        doc.add(new Paragraph("SCORE DE FORME GLOBAL").setTextAlignment(TextAlignment.CENTER).setFontSize(14).setFontColor(GRIS_TXT));
        DeviceRgb scoreColor = (bilan.getScoreForme() >= 7) ? VERT : (bilan.getScoreForme() >= 5 ? new DeviceRgb(230, 126, 34) : ROUGE);
        doc.add(new Paragraph(String.format("%.1f / 10", bilan.getScoreForme())).setFontSize(50).setBold().setFontColor(scoreColor).setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph("RISQUE BURNOUT : " + (bilan.isRisqueBurnout() ? "⚠️ ÉLEVÉ" : "✅ FAIBLE")).setBold().setFontSize(18).setTextAlignment(TextAlignment.CENTER).setFontColor(bilan.isRisqueBurnout() ? ROUGE : VERT));
    }

    private void ajouterGraphiques(Document doc, ChartImages charts) {
        doc.add(new Paragraph("ANALYSE VISUELLE").setBold().setFontSize(18).setFontColor(VIOLET).setMarginBottom(20));

        if (charts.lineChartBytes != null) {
            doc.add(new Paragraph("Évolution du score de forme").setBold().setFontSize(12));
            doc.add(new Image(ImageDataFactory.create(charts.lineChartBytes)).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20));
        }

        if (charts.barChartBytes != null) {
            doc.add(new Paragraph("Historique du sommeil (Heures)").setBold().setFontSize(12));
            doc.add(new Image(ImageDataFactory.create(charts.barChartBytes)).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20));
        }

        if (charts.radarBytes != null) {
            doc.add(new Paragraph("Équilibre des indicateurs santé").setBold().setFontSize(12));
            doc.add(new Image(ImageDataFactory.create(charts.radarBytes)).setWidth(UnitValue.createPercentValue(60)).setHorizontalAlignment(HorizontalAlignment.CENTER));
        }
    }

    private void ajouterTableauSuivis(Document doc, List<SuiviSante> suivis) {
        doc.add(new Paragraph("JOURNAL DES SUIVIS").setBold().setFontSize(18).setFontColor(VIOLET).setMarginBottom(15));

        Table table = new Table(UnitValue.createPercentArray(new float[]{15, 15, 10, 10, 30, 10, 10})).useAllAvailableWidth();
        String[] headers = {"Date", "Sommeil", "Eau", "Hum.", "Activité", "Poids", "Notes"};

        for (String h : headers) {
            table.addHeaderCell(new Cell().setBackgroundColor(VIOLET).add(new Paragraph(h).setFontColor(ColorConstants.WHITE).setBold().setFontSize(9)));
        }

        for (SuiviSante s : suivis) {
            table.addCell(new Cell().add(new Paragraph(s.getDate().toString()).setFontSize(8)));
            table.addCell(new Cell().add(new Paragraph(s.getHeuresSommeil() + "h").setFontSize(8)));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(s.getVerresEau())).setFontSize(8)));
            table.addCell(new Cell().add(new Paragraph(s.getHumeur() + "/10").setFontSize(8)));
            table.addCell(new Cell().add(new Paragraph(s.getActivite() + " (" + s.getMinutesActivite() + "m)").setFontSize(8)));
            table.addCell(new Cell().add(new Paragraph(s.getPoids() + "kg").setFontSize(8)));
            table.addCell(new Cell().add(new Paragraph(s.getNotes()).setFontSize(8)));
        }
        doc.add(table);
    }

    private void ajouterRecommandations(Document doc, BilanSante bilan) {
        doc.add(new Paragraph("RECOMMANDATIONS IA").setBold().setFontSize(18).setFontColor(VIOLET).setMarginBottom(15));

        String recs = bilan.getRecommandations();
        if (recs != null && !recs.isEmpty()) {
            doc.add(new Paragraph(recs).setFontSize(11).setBackgroundColor(GRIS_FOND).setPadding(15));
        } else {
            doc.add(new Paragraph("Aucune recommandation générée pour ce bilan.").setItalic().setFontColor(GRIS_TXT));
        }

        doc.add(new Paragraph("\n\nCe rapport a été généré automatiquement par LifeOps IA.").setFontSize(9).setFontColor(GRIS_TXT).setTextAlignment(TextAlignment.CENTER));
    }

    private void ajouterNumerosPages(PdfDocument pdf, Document doc) {
        int nbPages = pdf.getNumberOfPages();
        for (int i = 1; i <= nbPages; i++) {
            doc.showTextAligned(new Paragraph("Page " + i + " / " + nbPages).setFontSize(8).setFontColor(GRIS_TXT), 297, 20, i, TextAlignment.CENTER, VerticalAlignment.BOTTOM, 0);
        }
    }

    public record ChartImages(byte[] lineChartBytes, byte[] barChartBytes, byte[] radarBytes) {}

    // ── Standalone entry point (no BilanSante / charts required) ─────

    /**
     * Generates a self-contained PDF report directly from suivi data.
     * Called by DashboardSanteController when the user clicks "Rapport PDF".
     *
     * @param outputPath absolute path for the output .pdf file
     * @param suivis     list of SuiviSante records (newest first)
     */
    public void generer(String outputPath, List<SuiviSante> suivis) throws IOException {
        BilanSante bilan = construireBilanDepuisSuivis(suivis);

        PdfWriter writer = new PdfWriter(outputPath);
        PdfDocument pdf  = new PdfDocument(writer);
        Document doc     = new Document(pdf, PageSize.A4, false);
        doc.setMargins(36, 40, 36, 40);

        ajouterCouverture(doc, bilan, suivis.size());
        doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        ajouterTableauSuivis(doc, suivis);
        doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        ajouterRecommandations(doc, bilan);

        ajouterNumerosPages(pdf, doc);
        doc.close();
    }

    /** Derives a BilanSante summary from raw suivi records. */
    private BilanSante construireBilanDepuisSuivis(List<SuiviSante> suivis) {
        BilanSante b = new BilanSante();

        if (suivis.isEmpty()) {
            b.setDateDebut(java.time.LocalDate.now());
            b.setDateFin(java.time.LocalDate.now());
            b.setScoreForme(0);
            b.setRisqueBurnout(false);
            b.setRecommandations("Aucune donnée disponible.");
            return b;
        }

        // Date range (data is newest-first)
        b.setDateFin(suivis.get(0).getDate());
        b.setDateDebut(suivis.get(suivis.size() - 1).getDate());

        double avgSommeil  = suivis.stream().mapToDouble(s -> s.getHeuresSommeil()).average().orElse(0);
        double avgQualite  = suivis.stream().mapToDouble(s -> s.getQualiteSommeil()).average().orElse(0);
        double avgEau      = suivis.stream().mapToDouble(s -> s.getVerresEau()).average().orElse(0);
        double avgActivite = suivis.stream().mapToDouble(s -> s.getMinutesActivite()).average().orElse(0);
        double avgHumeur   = suivis.stream().mapToDouble(s -> s.getHumeur()).average().orElse(0);

        // Score /10 for BilanSante (existing field is /10)
        double score100 = calculerScore100(avgSommeil, (int) avgEau, (int) avgActivite, (int) avgHumeur);
        b.setScoreForme((float) (score100 / 10.0));

        // Burnout
        int risque = 0;
        if (avgSommeil < 6)   risque += 2; else if (avgSommeil < 7) risque += 1;
        if (avgQualite <= 2)  risque += 2; else if (avgQualite == 3) risque += 1;
        if (avgHumeur  <= 2)  risque += 2; else if (avgHumeur  == 3) risque += 1;
        if (avgActivite < 15) risque += 1;
        b.setRisqueBurnout(risque >= 4);

        // Recommendations text
        StringBuilder recs = new StringBuilder();
        if (avgSommeil < 7)
            recs.append("• Sommeil insuffisant (moy. ").append(String.format("%.1f", avgSommeil))
                .append("h). Visez 7–9h par nuit.\n");
        if (avgEau < 6)
            recs.append("• Hydratation faible (moy. ").append(String.format("%.0f", avgEau))
                .append(" verres/j). Objectif : 8 verres.\n");
        if (avgActivite < 20)
            recs.append("• Activité physique insuffisante (moy. ").append(String.format("%.0f", avgActivite))
                .append(" min/j). Essayez 30 min de marche quotidienne.\n");
        if (avgHumeur < 3)
            recs.append("• Humeur basse (moy. ").append(String.format("%.1f", avgHumeur))
                .append("/5). Pratiquez des activités qui vous font du bien.\n");
        if (recs.length() == 0)
            recs.append("✅ Tous vos indicateurs sont dans les normes. Continuez ainsi !");

        b.setRecommandations(recs.toString());
        return b;
    }

    private double calculerScore100(double sommeil, int eau, int activite, int humeur) {
        double s = Math.max(0, 25 - Math.abs(sommeil - 8) * 5);
        double e = Math.min(20, (eau      / 8.0)  * 20);
        double a = Math.min(20, (activite / 30.0) * 20);
        double h = (humeur / 5.0) * 15;
        return Math.min(100, s + e + a + h + 20);
    }
}
