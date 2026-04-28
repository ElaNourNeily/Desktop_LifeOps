package service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ReceiptOcrService {

    private static final String ENV_TESSDATA = "LIFEOPS_TESSDATA";

    // Keep models small-ish by default.
    private static final String TESSDATA_FAST_BASE =
            "https://github.com/tesseract-ocr/tessdata_fast/raw/main/";

    /**
     * tess4j relies on native Tesseract libraries. On macOS (especially Apple Silicon),
     * those libraries are typically installed via Homebrew and may not be on the default
     * Java/JNA lookup path, so we try a couple of common locations.
     */
    private static final List<Path> MAC_NATIVE_CANDIDATES = List.of(
            Paths.get("/opt/homebrew/lib/libtesseract.dylib"),
            Paths.get("/usr/local/lib/libtesseract.dylib")
    );

    public String ocrReceipt(File imageFile) throws IOException, TesseractException {
        Objects.requireNonNull(imageFile, "imageFile");
        if (!imageFile.exists()) {
            throw new IOException("Fichier introuvable: " + imageFile.getAbsolutePath());
        }

        ensureNativeTesseractLoadedOrThrow();
        Path tessdataDir = resolveOrPrepareTessdata();

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataDir.toAbsolutePath().toString());

        // Prefer FR if available, otherwise ENG is still good enough for totals/dates.
        List<String> langs = new ArrayList<>();
        if (Files.exists(tessdataDir.resolve("fra.traineddata"))) {
            langs.add("fra");
        }
        langs.add("eng");
        tesseract.setLanguage(String.join("+", langs));

        // Reasonable defaults for receipts.
        tesseract.setPageSegMode(6);
        tesseract.setOcrEngineMode(1);
        tesseract.setTessVariable("user_defined_dpi", "300");

        File ocrInput = imageFile;
        Path tmp = null;
        try {
            if (isHeicLike(imageFile)) {
                tmp = convertHeicToPng(imageFile);
                ocrInput = tmp.toFile();
            }
            return tesseract.doOCR(ocrInput);
        } catch (UnsatisfiedLinkError e) {
            // Defensive: if native loading still fails here, translate to a clear hint.
            throw new IOException(nativeErrorHint(e), e);
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private boolean isHeicLike(File f) {
        String n = f.getName().toLowerCase();
        return n.endsWith(".heic") || n.endsWith(".heif");
    }

    private Path convertHeicToPng(File heicFile) throws IOException {
        // On macOS, `sips` is available by default and can convert HEIC/HEIF to PNG.
        // If you're not on macOS or sips isn't available, ask user to convert to JPG/PNG.
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("mac")) {
            throw new IOException("Format HEIC/HEIF non supporte ici. Convertis l'image en PNG/JPG avant de scanner.");
        }

        Path out = Files.createTempFile("lifeops-receipt-" + UUID.randomUUID(), ".png");
        ProcessBuilder pb = new ProcessBuilder(
                "sips",
                "-s", "format", "png",
                heicFile.getAbsolutePath(),
                "--out", out.toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);

        try {
            Process p = pb.start();
            int code = p.waitFor();
            if (code != 0 || !Files.exists(out) || Files.size(out) == 0) {
                throw new IOException("Conversion HEIC -> PNG echouee (code " + code + ").");
            }
            return out;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Conversion HEIC interrompue.", e);
        }
    }

    private Path resolveOrPrepareTessdata() throws IOException {
        // 1) Explicit override.
        String env = System.getenv(ENV_TESSDATA);
        if (env != null && !env.isBlank()) {
            Path p = Paths.get(env.trim());
            if (Files.isDirectory(p) && Files.exists(p.resolve("eng.traineddata"))) {
                return p;
            }
        }

        // 2) User-home cache.
        Path home = Paths.get(System.getProperty("user.home"), ".lifeops", "tessdata");
        try {
            ensureTrainedData(home);
        } catch (IOException e) {
            throw new IOException(
                    "OCR: tessdata introuvable et telechargement impossible.\n" +
                    "Option 1: Assure-toi d'avoir Internet au premier scan.\n" +
                    "Option 2: Installe Tesseract et pointe le dossier tessdata via la variable d'env " + ENV_TESSDATA +
                    " (ex: /usr/local/share/tessdata ou /opt/homebrew/share/tessdata).\n" +
                    "Details: " + e.getMessage(),
                    e
            );
        }
        return home;
    }

    public boolean isNativeTesseractPresent() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            for (Path p : MAC_NATIVE_CANDIDATES) {
                if (Files.exists(p)) {
                    return true;
                }
            }
            return false;
        }
        // For non-mac platforms, we don't have a reliable file path check here.
        // The best signal is still attempting to load at runtime.
        return true;
    }

    public String nativeInstallHintShort() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            return "OCR indisponible: installe Tesseract (Homebrew) puis relance l'app: `brew install tesseract`.";
        }
        if (os.contains("win")) {
            return "OCR indisponible: installe Tesseract (Windows) puis relance l'app.";
        }
        return "OCR indisponible: installe Tesseract (tesseract-ocr) puis relance l'app.";
    }

    private void ensureNativeTesseractLoadedOrThrow() throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("mac")) {
            return;
        }

        // If the dylib is present, try to load it and also help JNA by setting the library path.
        Path candidate = null;
        for (Path p : MAC_NATIVE_CANDIDATES) {
            if (Files.exists(p)) {
                candidate = p;
                break;
            }
        }

        if (candidate == null) {
            throw new IOException(nativeInstallHintShort());
        }

        Path dir = candidate.getParent();
        if (dir != null) {
            // Let JNA know where to find dependencies (liblept, etc.).
            String existing = System.getProperty("jna.library.path", "");
            if (existing == null || existing.isBlank()) {
                System.setProperty("jna.library.path", dir.toString());
            } else if (!existing.contains(dir.toString())) {
                System.setProperty("jna.library.path", existing + File.pathSeparator + dir);
            }
        }

        try {
            System.load(candidate.toAbsolutePath().toString());
        } catch (UnsatisfiedLinkError e) {
            throw new IOException(nativeErrorHint(e), e);
        }
    }

    private String nativeErrorHint(Throwable e) {
        String os = System.getProperty("os.name", "").toLowerCase();
        String base = e == null ? "Erreur OCR native." : e.getMessage();
        if (os.contains("mac")) {
            return "OCR indisponible: bibliotheque native Tesseract introuvable/chargee.\n" +
                   "Installe: `brew install tesseract`.\n" +
                   "Si deja installe, lance l'app avec les libs visibles (ex: DYLD_LIBRARY_PATH=/opt/homebrew/lib).\n" +
                   "Details: " + base;
        }
        return "OCR indisponible: bibliotheque native Tesseract introuvable/chargee.\n" +
               "Installe tesseract-ocr puis relance.\n" +
               "Details: " + base;
    }

    private void ensureTrainedData(Path tessdataDir) throws IOException {
        Files.createDirectories(tessdataDir);

        // Minimum requirement: eng.
        if (!Files.exists(tessdataDir.resolve("eng.traineddata"))) {
            downloadTrainedData(tessdataDir, "eng");
        }

        // Optional: French helps in FR receipts.
        if (!Files.exists(tessdataDir.resolve("fra.traineddata"))) {
            try {
                downloadTrainedData(tessdataDir, "fra");
            } catch (IOException ignored) {
                // Don't fail the whole flow if french model download fails.
            }
        }
    }

    private void downloadTrainedData(Path tessdataDir, String lang) throws IOException {
        String file = lang + ".traineddata";
        Path target = tessdataDir.resolve(file);
        URI uri = URI.create(TESSDATA_FAST_BASE + file);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        try {
            HttpResponse<Path> resp = client.send(request, HttpResponse.BodyHandlers.ofFile(target));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new IOException("Telechargement tessdata echoue (" + resp.statusCode() + "): " + uri);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Telechargement interrompu: " + uri, e);
        }
    }
}
