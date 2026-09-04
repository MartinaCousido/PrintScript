package org.printscript.semantic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TypeTest {

    private static final String NUMBER = "number";

    private static final Set<Type> DE_LA_10 = Set.of(Type.NUMBER, Type.STRING);

    @Test
    void translatesTheWrittenAnnotationToTheSemanticType() {
        assertEquals(Optional.of(Type.NUMBER), Type.fromWrittenName(NUMBER, DE_LA_10));
        assertEquals(Optional.of(Type.STRING), Type.fromWrittenName("string", DE_LA_10));
    }

    @Test
    void anAnnotationThatIsNotATypeHasNoTranslation() {
        assertEquals(Optional.empty(), Type.fromWrittenName("Number", DE_LA_10));
        assertEquals(Optional.empty(), Type.fromWrittenName("", DE_LA_10));
    }

    @Test
    void aTypeThatTheVersionDoesNotHaveIsAnUnknownAnnotation() {
        // el mismo texto es un tipo válido en una versión y una anotación desconocida
        // en otra: por eso la traducción se hace contra el vocabulario de la versión
        assertEquals(Optional.empty(), Type.fromWrittenName(NUMBER, Set.of(Type.STRING)));
        assertEquals(Optional.of(Type.NUMBER), Type.fromWrittenName(NUMBER, Set.of(Type.NUMBER)));
    }

    @Test
    void everyTypeKnowsHowItIsWritten() {
        assertEquals(NUMBER, Type.NUMBER.writtenName());
        assertEquals("string", Type.STRING.writtenName());
    }
}
