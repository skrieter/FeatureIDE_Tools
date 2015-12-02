package de.ovgu.featureide.fm.test;


import java.util.Collection;

import org.prop4j.Node;
import org.xtext.example.mydsl.fML.SliceMode;

import de.ovgu.featureide.fm.core.FeatureModel;
import fr.familiar.fm.featureide.FeatureIDEtoFML;
import fr.familiar.gui.Converter;
import fr.familiar.operations.featureide.FMSlicerSAT;
import fr.familiar.operations.featureide.SATBuilder;
import fr.familiar.variable.FeatureModelVariable;

public class Familiar {
	
	public static Node createNodes(FeatureModel featureModel, Collection<String> removeFeatures) {
		final FeatureIDEtoFML toFML = new FeatureIDEtoFML(featureModel);
		FeatureModelVariable fmv = Converter.INSTANCE.createNewFMV(Converter.INSTANCE.getInternalFM(toFML.writeToString()), "eval");
		fmv = new FMSlicerSAT().sliceFM(fmv, removeFeatures, SliceMode.EXCLUDING);
		return fmv == null ? null : new SATBuilder().mkNode(fmv);
	}
}
