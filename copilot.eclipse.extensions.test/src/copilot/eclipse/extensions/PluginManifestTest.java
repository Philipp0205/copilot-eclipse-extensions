package copilot.eclipse.extensions;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Guards the contribution wiring in {@code plugin.xml}.
 *
 * <p>
 * Eclipse only reports a bad class name or missing translation key when a user triggers the
 * contribution, so these are checked against the packaged bundle instead. The entries are looked up
 * as bundle resources rather than loaded as classes, because loading them would start the Copilot
 * bundles and their language server.
 * </p>
 */
class PluginManifestTest {

  private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]+)\"");
  private static final Pattern PROPERTY_REFERENCE = Pattern.compile("\"%([^\"]+)\"");

  private static Bundle bundle;
  private static String pluginXml;
  private static Properties pluginProperties;

  @BeforeAll
  static void readBundleContents() throws IOException {
    bundle = FrameworkUtil.getBundle(PluginManifestTest.class);
    assertNotNull(bundle, "test fragment must run inside its host bundle");

    pluginXml = read(bundle.getEntry("/plugin.xml"));
    pluginProperties = new Properties();
    try (InputStream in = bundle.getEntry("/plugin.properties").openStream()) {
      pluginProperties.load(in);
    }
  }

  @Test
  void declaresOnlyClassesThatArePackaged() {
    Set<String> classNames = matches(CLASS_ATTRIBUTE, pluginXml);

    assertTrue(classNames.size() >= 12, "expected the contributions to be found, got " + classNames);
    for (String className : classNames) {
      String path = "/" + className.replace('.', '/') + ".class";
      assertNotNull(bundle.getEntry(path), className + " is declared in plugin.xml but not packaged");
    }
  }

  @Test
  void declaresOnlyTranslationKeysThatExist() {
    for (String key : matches(PROPERTY_REFERENCE, pluginXml)) {
      assertNotNull(pluginProperties.getProperty(key),
          key + " is referenced in plugin.xml but missing from plugin.properties");
    }
  }

  @Test
  void bindsEveryCommandToAHandler() {
    Set<String> commandIds = matches(Pattern.compile("<command\\s+id=\"([^\"]+)\""), pluginXml);
    Set<String> handledIds = matches(Pattern.compile("commandId=\"([^\"]+)\""), pluginXml);

    assertTrue(commandIds.size() >= 7, "expected the commands to be found, got " + commandIds);
    for (String commandId : commandIds) {
      assertTrue(handledIds.contains(commandId), commandId + " has no handler or menu contribution");
    }
  }

  private static Set<String> matches(Pattern pattern, String text) {
    Set<String> found = new LinkedHashSet<>();
    Matcher matcher = pattern.matcher(text);
    while (matcher.find()) {
      found.add(matcher.group(1));
    }
    return found;
  }

  private static String read(URL url) throws IOException {
    assertNotNull(url, "missing bundle entry");
    try (InputStream in = url.openStream()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
