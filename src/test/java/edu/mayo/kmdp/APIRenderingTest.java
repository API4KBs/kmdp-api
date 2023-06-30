package edu.mayo.kmdp;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.davidmoten.oas3.puml.Converter;
import edu.mayo.kmdp.util.FileUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.swing.SwingUtilities;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class APIRenderingTest {

  @TempDir
  File tempDir;

  static final List<String> apiPaths = List.of(
      "/openapi/v3/edu/mayo/kmdp/1.0.0/wemi.oas.yaml",
      "/openapi/v3/edu/mayo/kmdp/3.0.0/conceptGlossaryLibrary.oas.yaml",
      "/openapi/v3/edu/mayo/kmdp/5.0.0/terminology.oas.yaml",

      "/openapi/v3/org/omg/api4kp/5.0.0/knowledgeArtifactRepository.oas.yaml",
      "/openapi/v3/org/omg/api4kp/5.0.0/knowledgeAssetRepository.oas.yaml",
      "/openapi/v3/org/omg/api4kp/5.0.0/knowledgeBaseConstruction.oas.yaml",
      "/openapi/v3/org/omg/api4kp/5.0.0/knowledgeReasoning.oas.yaml"
  );

  public static void main(String[] args) throws URISyntaxException, IOException {
    new APIRenderingTest()
        .run(apiPaths.get(0));
  }


  private void run(String path) throws URISyntaxException, IOException {
    var svg = getSvg(path);
    assertNotNull(svg);

    File f = tempFile();
    FileUtil.write(svg.getBytes(), f);
    var url = f.toURI().toURL();

    SwingUtilities.invokeLater(()
        -> new SVGRenderer().loadSVG(url));
  }

  private File tempFile() throws URISyntaxException, IOException {
    if (tempDir == null) {
      var base = APIRenderingTest.class.getResource("/");
      assertNotNull(base);
      var tmpPath = Path.of(base.toURI()).resolve("target").resolve("tempSvg");
      Files.createDirectories(tmpPath);
      tempDir = tmpPath.toFile();
    }
    return new File(tempDir, "temp.svg");
  }


  @ParameterizedTest
  @MethodSource("provideApiSources")
  void testDiagram(String source) {
    assertNotNull(getSvg(source));
  }

  private static Stream<Arguments> provideApiSources() {
    return apiPaths.stream()
        .map(Arguments::of);
  }


  private static String getSvg(String source) {
    var res = APIRenderingTest.class.getResourceAsStream(source);
    assertNotNull(res);

    try (var os = new ByteArrayOutputStream()) {
      String puml = Converter.openApiToPuml(res)
          .replace("@startuml", "@startuml\n !pragma layout smetana");

      SourceStringReader reader = new SourceStringReader(puml);
      var desc = reader.outputImage(os, new FileFormatOption(FileFormat.SVG));
      assertNotNull(desc);
      return os.toString(StandardCharsets.UTF_8);
    } catch (Exception e) {
      fail(e);
      return null;
    }
  }


}
