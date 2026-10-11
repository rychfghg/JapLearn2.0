package japlearn.demo.Service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextInputTest {

    @Test
    void acceptsRealNamesInAnyLanguage() {
        assertThat(TextInput.name("  Maria   Santos ")).isEqualTo("Maria Santos");
        assertThat(TextInput.name("O'Neil-Cruz")).isEqualTo("O'Neil-Cruz");
        assertThat(TextInput.name("Ma. Teresa")).isEqualTo("Ma. Teresa");
        assertThat(TextInput.name("José")).isEqualTo("José");
        assertThat(TextInput.name("さくら")).isEqualTo("さくら");
        assertThat(TextInput.name("山田・タロウ")).isEqualTo("山田・タロウ");
    }

    @Test
    void rejectsEmojisNumbersSymbolsAndMarkup() {
        assertThat(TextInput.name("Maria 😀")).isNull();
        assertThat(TextInput.name("😀")).isNull();
        assertThat(TextInput.name("Ana❤")).isNull();
        assertThat(TextInput.name("Ana‍")).isNull();      // invisible joiner
        assertThat(TextInput.name("Ana123")).isNull();
        assertThat(TextInput.name("<b>Ana</b>")).isNull();
        assertThat(TextInput.name("")).isNull();
        assertThat(TextInput.name("-Ana")).isNull();
        assertThat(TextInput.name("A".repeat(61))).isNull();
    }

    @Test
    void passwordsUseKeyboardCharactersOnly() {
        assertThat(TextInput.validPassword("Secret#2026 ok")).isTrue();
        assertThat(TextInput.validPassword("short1!")).isFalse();
        assertThat(TextInput.validPassword("password😀123")).isFalse();
        assertThat(TextInput.validPassword("pässwört123")).isFalse();
        assertThat(TextInput.validPassword("x".repeat(129))).isFalse();
    }
}
