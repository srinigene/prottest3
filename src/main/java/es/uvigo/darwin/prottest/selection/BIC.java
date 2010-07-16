/** 
 * AIC.java
 *
 * Description:		AIC computation
 * @author			David Posada, University of Vigo, Spain  
 *					dposada@uvigo.es | darwin.uvigo.es
 */


package es.uvigo.darwin.prottest.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import es.uvigo.darwin.prottest.model.Model;
import es.uvigo.darwin.prottest.selection.model.BICSelectionModel;
import es.uvigo.darwin.prottest.selection.model.SelectionModel;
import es.uvigo.darwin.prottest.util.collection.ModelCollection;

/**
 * The Bayesian Information Criterion.
 */
public class BIC extends InformationCriterion
{
	
//	/**
//	 * Instantiates a new Bayesian Information Criterion.
//	 * 
//	 * @param models the models
//	 * @param confidenceInterval the confidence interval
//	 */
//	public BIC (ModelCollection models, double confidenceInterval) 
//		{
//		super(models, confidenceInterval);
//		
//		Collections.sort(selectionModels);
//		}
	
	/**
	 * Instantiates a new Bayesian Information Criterion.
	 * 
	 * @param models the models
	 * @param confidenceInterval the confidence interval
	 * @param sampleSize the sample size if different of the default
	 */
	public BIC (ModelCollection models, double confidenceInterval, double sampleSize) 
		{
		super(models, confidenceInterval, sampleSize);
		
		Collections.sort(selectionModels);
		}
	
	/* (non-Javadoc)
	 * @see es.uvigo.darwin.prottest.selection.InformationCriterion#getSelectionModels(es.uvigo.darwin.prottest.util.collection.ModelIterator)
	 */
	protected List<SelectionModel> getSelectionModels(List<Model> models) {
		List<SelectionModel> list = new ArrayList<SelectionModel>();
		for (Model model : models) {
			SelectionModel toAdd = new BICSelectionModel(
					model, 
					sampleSize); 
			list.add( toAdd );
			hashModels.put(model, toAdd);
		}
		return list;
	}

	/* (non-Javadoc)
	 * @see es.uvigo.darwin.prottest.selection.InformationCriterion#getCriterionName()
	 */
	public String getCriterionName() {
		return "BIC";
	}
}


