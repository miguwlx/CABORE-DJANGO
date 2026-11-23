package co.cabore.CaboreApplication.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Subida segura de imágenes para productos, logos y banners.
 *
 * Reglas (idénticas a guardar_producto.php y actualizar_tienda.php):
 *  - Extensiones permitidas: jpg, jpeg, png, webp, gif.
 *  - Tamaño máximo: 5 MB.
 *  - Verificación de firma binaria (magic bytes) para impedir archivos ejecutables
 *    o scripts disfrazados con extensión de imagen.
 *  - Nombre aleatorio sin caracteres del usuario (evita path traversal).
 *  - Devuelve la ruta relativa "img/<subcarpeta>/<prefijo>_<uuid>.<ext>",
 *    compatible con el formato del PHP original y con Rutas.publica().
 */
@Service
public class ImagenService {

    public static final long MAX_BYTES = 5L * 1024L * 1024L; // 5 MB
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final Path baseDir;

    public ImagenService(@Value("${cabore.uploads.dir:uploads/img}") String uploadsDir) {
        this.baseDir = Paths.get(uploadsDir).toAbsolutePath().normalize();
    }

    public Path getBaseDir() {
        return baseDir;
    }

    /**
     * Guarda la imagen subida y devuelve la ruta relativa para almacenar en BD
     * (p. ej. "img/productos/prod_a1b2c3.jpg"), o null si no se envió archivo.
     */
    public String guardar(MultipartFile archivo, String subcarpeta, String prefijo) {
        return guardarOptimizada(archivo, subcarpeta, prefijo, 1600, 1600);
    }

    /**
     * Guarda la imagen validando firma binaria y tamaño máximo, y redimensiona
     * automáticamente imágenes JPG/PNG que superen las dimensiones máximas indicadas
     * para optimizar el peso en la tienda pública.
     */
    public String guardarOptimizada(MultipartFile archivo,
                                    String subcarpeta,
                                    String prefijo,
                                    int maxAncho,
                                    int maxAlto) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new OperacionInvalidaException("La imagen supera el tamaño máximo permitido de 5 MB.");
        }

        String original = archivo.getOriginalFilename();
        String ext = extraerExtension(original);
        if (!EXTENSIONES_PERMITIDAS.contains(ext)) {
            throw new OperacionInvalidaException("Formato de imagen no permitido. Usa JPG, PNG, WEBP o GIF.");
        }

        try {
            byte[] cabecera = leerCabecera(archivo);
            if (!esFirmaImagenValida(cabecera)) {
                throw new OperacionInvalidaException("El archivo subido no es una imagen válida.");
            }

            Path destinoDir = baseDir.resolve(subcarpeta).normalize();
            if (!destinoDir.startsWith(baseDir)) {
                throw new OperacionInvalidaException("Ruta de destino inválida.");
            }
            Files.createDirectories(destinoDir);

            String token = UUID.randomUUID().toString().replace("-", "").substring(0, 13);
            String filename = prefijo + "_" + token + "." + ext;
            Path destinoArchivo = destinoDir.resolve(filename).normalize();

            boolean escritaOptimizada = false;
            if (("jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext)) && maxAncho > 0 && maxAlto > 0) {
                escritaOptimizada = intentarOptimizarImagen(archivo, destinoArchivo, ext, maxAncho, maxAlto);
            }
            if (!escritaOptimizada) {
                try (InputStream in = archivo.getInputStream()) {
                    Files.copy(in, destinoArchivo, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            return "img/" + subcarpeta + "/" + filename;
        } catch (IOException e) {
            throw new OperacionInvalidaException("No se pudo guardar la imagen.");
        }
    }

    private boolean intentarOptimizarImagen(MultipartFile archivo,
                                            Path destino,
                                            String ext,
                                            int maxAncho,
                                            int maxAlto) {
        try (InputStream in = archivo.getInputStream()) {
            java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(in);
            if (original == null) {
                return false;
            }
            int w = original.getWidth();
            int h = original.getHeight();
            if (w <= maxAncho && h <= maxAlto) {
                return false; // Ya cumple las dimensiones óptimas; conservar original sin pérdida
            }
            double escala = Math.min((double) maxAncho / w, (double) maxAlto / h);
            int nuevoW = Math.max(1, (int) Math.round(w * escala));
            int nuevoH = Math.max(1, (int) Math.round(h * escala));

            boolean conAlpha = "png".equals(ext);
            java.awt.image.BufferedImage redim = new java.awt.image.BufferedImage(
                    nuevoW, nuevoH,
                    conAlpha ? java.awt.image.BufferedImage.TYPE_INT_ARGB : java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = redim.createGraphics();
            try {
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                        java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                if (!conAlpha) {
                    g.setColor(java.awt.Color.WHITE);
                    g.fillRect(0, 0, nuevoW, nuevoH);
                }
                g.drawImage(original, 0, 0, nuevoW, nuevoH, null);
            } finally {
                g.dispose();
            }

            String formato = "png".equals(ext) ? "png" : "jpg";
            return javax.imageio.ImageIO.write(redim, formato, destino.toFile());
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String extraerExtension(String nombre) {
        if (nombre == null) return "";
        String limpio = nombre.replace('\\', '/');
        int slash = limpio.lastIndexOf('/');
        if (slash >= 0) limpio = limpio.substring(slash + 1);
        int dot = limpio.lastIndexOf('.');
        if (dot < 0 || dot == limpio.length() - 1) return "";
        return limpio.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static byte[] leerCabecera(MultipartFile archivo) throws IOException {
        try (InputStream in = archivo.getInputStream()) {
            return in.readNBytes(12);
        }
    }

    /** Comprueba los magic bytes de JPEG, PNG, GIF y WEBP (RIFF....WEBP). */
    private static boolean esFirmaImagenValida(byte[] b) {
        if (b == null || b.length < 4) return false;
        // JPEG: FF D8 FF
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47
        if ((b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47) {
            return true;
        }
        // GIF: "GIF8"
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return true;
        }
        // WEBP: "RIFF" .... "WEBP"
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return true;
        }
        return false;
    }
}
