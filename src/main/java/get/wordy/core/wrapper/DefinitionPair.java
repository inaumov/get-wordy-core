package get.wordy.core.wrapper;

public class DefinitionPair<Definition, Meaning> {

    private final Definition definition;
    private final Meaning[] meanings;

    public static <Definition, Meaning> DefinitionPair<Definition, Meaning> createPair(Definition d, Meaning[] m) {
        return new DefinitionPair<Definition, Meaning>(d, m);
    }

    public DefinitionPair(Definition definition, Meaning[] meanings) {
        this.definition = definition;
        this.meanings = meanings;
    }

    public Definition getDefinition() {
        return definition;
    }

    public Meaning[] getMeanings() {
        return meanings;
    }

}