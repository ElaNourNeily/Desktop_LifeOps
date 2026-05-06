package service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import model.BilanSante;
import model.SuiviSante;

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
}
