package utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PdfChartRenderer {

    public byte[] nodeToBytes(Node node, double width, double height) throws IOException {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.web("#1e293b")); 

        WritableImage image = new WritableImage((int)(width * 2), (int)(height * 2));
        node.snapshot(params, image);

        BufferedImage buffered = SwingFXUtils.fromFXImage(image, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(buffered, "PNG", baos);
        return baos.toByteArray();
    }

    public byte[] canvasToBytes(Canvas canvas) throws IOException {
        return nodeToBytes(canvas, canvas.getWidth(), canvas.getHeight());
    }
}
