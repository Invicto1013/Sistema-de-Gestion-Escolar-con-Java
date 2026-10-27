package com.unev.sige.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Vigila que todos los scripts de la carpeta sql/ estén en UTF-8 limpio y declaren utf8mb4,
 * para que las tildes y la ñ no vuelvan a guardarse mal (por ejemplo "Matemática" -> "MatemÃ¡tica").
 */
class ScriptsSqlUtf8Test {
    private static final Path CARPETA = Path.of("sql");

    private static List<Path> scripts() throws IOException {
        try (Stream<Path> s = Files.list(CARPETA)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".sql")).sorted().toList();
        }
    }

    private static String leerUtf8Estricto(Path archivo) throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(Files.readAllBytes(archivo))).toString();
        } catch (CharacterCodingException e) {
            throw new AssertionError(archivo + " no es UTF-8 válido: guárdalo como UTF-8", e);
        }
    }

    @Test
    void existenScriptsParaRevisar() throws IOException {
        assertTrue(scripts().size() >= 10, "se esperaba la carpeta sql/ con los scripts del proyecto");
    }

    @Test
    void todosSonUtf8ValidoYSinBom() throws IOException {
        for (Path p : scripts()) {
            byte[] bytes = Files.readAllBytes(p);
            assertFalse(bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF,
                    p + " tiene BOM: guárdalo como UTF-8 sin BOM");
            leerUtf8Estricto(p);
        }
    }

    @Test
    void ningunoContieneTextoCorrompidoPorCodificacion() throws IOException {
        Pattern mojibake = Pattern.compile("Ã[\\u0080-\\u00BF]|Â[\\u00A0-\\u00BF]|â€|\\uFFFD");
        for (Path p : scripts()) {
            Matcher m = mojibake.matcher(leerUtf8Estricto(p));
            assertFalse(m.find(), p + " contiene caracteres corruptos cerca de «" + (m.hitEnd() ? "" : m.group()) + "»");
        }
    }

    @Test
    void todosDeclaranConexionUtf8mb4() throws IOException {
        Pattern declara = Pattern.compile("SET\\s+NAMES\\s+utf8mb4", Pattern.CASE_INSENSITIVE);
        Pattern viejo = Pattern.compile("SET\\s+NAMES\\s+utf8(?!mb4)", Pattern.CASE_INSENSITIVE);
        for (Path p : scripts()) {
            String texto = leerUtf8Estricto(p);
            assertTrue(declara.matcher(texto).find(), p + " debe incluir SET NAMES utf8mb4;");
            assertFalse(viejo.matcher(texto).find(), p + " usa utf8 (3 bytes) en lugar de utf8mb4");
        }
    }

    @Test
    void cadaTablaCreadaDeclaraUtf8mb4() throws IOException {
        Pattern crea = Pattern.compile("CREATE\\s+TABLE", Pattern.CASE_INSENSITIVE);
        Pattern charset = Pattern.compile("CHARSET\\s*=\\s*utf8mb4", Pattern.CASE_INSENSITIVE);
        for (Path p : scripts()) {
            String texto = leerUtf8Estricto(p);
            int tablas = (int) crea.matcher(texto).results().count();
            int declaradas = (int) charset.matcher(texto).results().count();
            assertEquals(tablas, declaradas, p + ": cada CREATE TABLE debe terminar con DEFAULT CHARSET=utf8mb4");
        }
    }
}
