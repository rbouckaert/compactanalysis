package methods.implementation;

import methods.MethodsText;

public class Frequencies implements MethodsText {

	@Override
	public Class type() {
		return beast.evolution.substitutionmodel.Frequencies.class;
	}

	@Override
	public String getModelDescription(Object o) {
		beast.evolution.substitutionmodel.Frequencies f = (beast.evolution.substitutionmodel.Frequencies) o;
		StringBuilder b = new StringBuilder();
		if (f.dataInput.get() != null) {
			b.append("empirical frequencies\n");
		} else if (f.frequenciesInput.get().isEstimatedInput.get()) {
			b.append("estimated frequencies\n");
		} else {
			b.append("fixed equal frequencies\n");
		}
		return b.toString();
	}

}