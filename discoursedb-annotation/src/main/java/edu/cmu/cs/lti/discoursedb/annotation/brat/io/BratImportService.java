package edu.cmu.cs.lti.discoursedb.annotation.brat.io;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.cmu.cs.lti.discoursedb.annotation.brat.io.BratThreadExport.AnnotationSourceType;
import edu.cmu.cs.lti.discoursedb.annotation.brat.io.BratThreadExport.EntityTypes;
import edu.cmu.cs.lti.discoursedb.annotation.brat.model.BratAnnotation;
import edu.cmu.cs.lti.discoursedb.annotation.brat.model.BratAnnotationType;
import edu.cmu.cs.lti.discoursedb.core.model.annotation.AnnotationInstance;
import edu.cmu.cs.lti.discoursedb.core.model.annotation.Feature;
import edu.cmu.cs.lti.discoursedb.core.model.macro.Content;
import edu.cmu.cs.lti.discoursedb.core.model.macro.Contribution;
import edu.cmu.cs.lti.discoursedb.core.model.macro.DiscoursePart;
import edu.cmu.cs.lti.discoursedb.core.service.annotation.AnnotationService;
import edu.cmu.cs.lti.discoursedb.core.service.macro.ContentService;
import edu.cmu.cs.lti.discoursedb.core.service.macro.ContributionService;
import edu.cmu.cs.lti.discoursedb.core.service.macro.DiscoursePartService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;

@Log4j
@Service
@Transactional(propagation= Propagation.REQUIRED, readOnly=false)
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class BratImportService {

	private final @NonNull ContributionService contribService;
	private final @NonNull ContentService contentService;
	private final @NonNull AnnotationService annoService;
	private final @NonNull DiscoursePartService dpService;
	
	public void importThread(String baseFileName, File annFile, File offsetFile, File versionsFile ) throws IOException{
				
		// get mapping from entity to offset
		TreeMap<Integer, Long> offsetToContributionId = getOffsetToIdMap(offsetFile, EntityTypes.CONTRIBUTION);
		TreeMap<Integer, Long> offsetToContentId = getOffsetToIdMap(offsetFile, EntityTypes.CONTENT);

		// keep track of versions of orginally exported annotations and features
		Map<String, DDBEntityInfo> annotationBratIdToDDB = getBratIdToDdbIdMap(versionsFile, AnnotationSourceType.ANNOTATION);
		Map<String, DDBEntityInfo> featureBratIdToDDB = getBratIdToDdbIdMap(versionsFile, AnnotationSourceType.FEATURE);

		
		/*
		 * Init ddb annotation stats for deletion handling
		 */		
		// Retrieve ids of existing annotations on contributions and current revision contents within the current DiscourseDB
		// to identify deletions. When importing the brat file, we remove every annotations/feature id we import
		// from this list. The remaining ids are associated with annotations/features that have been deleted in the Brat file.
		DiscoursePart dp = dpService.findOne(Long.parseLong(baseFileName.substring(baseFileName.lastIndexOf("_")+1))).get();			
		Set<Long> ddbAnnotationIds = new HashSet<>();
		Set<Long> ddbFeatureIds = new HashSet<>();
		extractAnnotationAndFeatureIds(annoService.findContributionAnnotationsByDiscoursePart(dp), ddbAnnotationIds, ddbFeatureIds);
		extractAnnotationAndFeatureIds(annoService.findCurrentRevisionAnnotationsByDiscoursePart(dp), ddbAnnotationIds, ddbFeatureIds);
		/* ----  */
		
		
		List<String> bratStandoffEncodedStrings =FileUtils.readLines(annFile);  
		//sorting in reverse order assures that Attribute annotations (A) are imported after text-bound annotations (T)
		Collections.sort(bratStandoffEncodedStrings, Collections.reverseOrder());
		for (String bratStandoffEncodedString : bratStandoffEncodedStrings) {

			// create BratAnnotation object from Brat-Stand-off-Encoded String
			// offset correction will be done later
			BratAnnotation anno = new BratAnnotation(bratStandoffEncodedString);				

			if (anno.getType() == BratAnnotationType.T) {					
				DDBEntityInfo entityInfo = annotationBratIdToDDB.get(anno.getFullAnnotationId());			
				
				// if the annotation covers a span of at least half of the length of the separator
				// AND is fully contained in the separator, we assume we are creating an entity annotation
				if (anno.getCoveredText().length() > (BratThreadExport.CONTRIB_SEPARATOR.length() / 2)
						&& BratThreadExport.CONTRIB_SEPARATOR.contains(anno.getCoveredText())) {
					/*
					 * CONTRIBUTION LABEL
					 * Load the contribution entity associated with the current offset range
					 */

					Entry<Integer, Long> offset = offsetToContributionId.floorEntry(anno.getBeginIndex());
					Contribution contrib = contribService.findOne(offset.getValue()).get();

					// check if annotation already existed before
					if (annotationBratIdToDDB.keySet().contains(anno.getFullAnnotationId())) {
						ddbAnnotationIds.remove(entityInfo.getId()); //update deletion stats

						AnnotationInstance existingAnno = annoService.findOneAnnotationInstance(entityInfo.getId()).get();

						//check if the anno version in the database still matches the anno version we initially exported 
						if(existingAnno.getEntityVersion()==entityInfo.getVersion()){
							//check for and apply changes
							if (existingAnno.getBeginOffset() != 0) {
								existingAnno.setBeginOffset(0);
							}
							if (existingAnno.getEndOffset() != 0) {
								existingAnno.setBeginOffset(0);
							}
							if (existingAnno.getType().equalsIgnoreCase(anno.getAnnotationLabel())) {
								existingAnno.setType(anno.getAnnotationLabel());
							}								
						}else{
							log.error("Entity changed in DiscourseDB since the data was last import but also changed in the exported file. Cannot import annotation.");
						}
						
					} else {
						// anno is new and didn't exist in ddb before
						AnnotationInstance newAnno = annoService.createTypedAnnotation(anno.getAnnotationLabel());
						annoService.addAnnotation(contrib, newAnno);
						//add to mapping file in case we also create a feature for this new annotation
						annotationBratIdToDDB.put(anno.getFullAnnotationId(), new DDBEntityInfo(newAnno.getId(), newAnno.getEntityVersion())); 
					}
				} else {
					/*
					 * SPAN ANNOTATION
					 * Load the content entity associated with the current offset range
					 */

					Entry<Integer, Long> offset = offsetToContentId.floorEntry(anno.getBeginIndex());
					Content content = contentService.findOne(offset.getValue()).get();

					// calculate offset corrected index values for span annotation
					int offsetCorrectedBeginIdx = anno.getBeginIndex() - offset.getKey() - BratThreadExport.CONTRIB_SEPARATOR.length() - 1;
					int offsetCorrectedEndIdx = anno.getEndIndex() - offset.getKey() - BratThreadExport.CONTRIB_SEPARATOR.length() - 1;

					// check if annotation already existed before
					if (annotationBratIdToDDB.keySet().contains(anno.getFullAnnotationId())) {
						ddbAnnotationIds.remove(entityInfo.getId()); //update deletion stats

						// Anno already existed. Check for changes.
						AnnotationInstance existingAnno = annoService.findOneAnnotationInstance(entityInfo.getId()).get();

						//check if the anno version in the database still matches the anno version we initially exported 
						if(existingAnno.getEntityVersion()==entityInfo.getVersion()){
							//check for and apply changes
							if (existingAnno.getBeginOffset() != offsetCorrectedBeginIdx) {
								existingAnno.setBeginOffset(offsetCorrectedBeginIdx);
							}
							if (existingAnno.getEndOffset() != offsetCorrectedEndIdx) {
								existingAnno.setBeginOffset(offsetCorrectedEndIdx);
							}
							if (existingAnno.getType().equalsIgnoreCase(anno.getAnnotationLabel())) {
								existingAnno.setType(anno.getAnnotationLabel());
							}								
						}else{
							log.error("Entity changed in DiscourseDB since the data was last import but also changed in the exported file. Cannot import annotation.");
						}

					} else {
						// Anno is new and didn't exist in ddb before. Create it.
						AnnotationInstance newAnno = annoService.createTypedAnnotation(anno.getAnnotationLabel());
						newAnno.setBeginOffset(offsetCorrectedBeginIdx);
						newAnno.setEndOffset(offsetCorrectedEndIdx);
						annoService.addAnnotation(content, newAnno);
					}
				}
				
			} else if (anno.getType() == BratAnnotationType.A) {
				
				DDBEntityInfo entityInfo = featureBratIdToDDB.get(anno.getFullAnnotationId());
				
				// check if feature already existed before
				if (featureBratIdToDDB.keySet().contains(anno.getFullAnnotationId())) {
					ddbFeatureIds.remove(entityInfo.getId()); //update deletion stats

					// feature already existed
					Feature existingFeature = annoService.findOneFeature(entityInfo.getId()).get();

					//check if the feature version in the database still matches the feature version we initially exported 
					if(existingFeature.getEntityVersion()==entityInfo.getVersion()){
						//check for and apply changes
						if(existingFeature.getValue().equalsIgnoreCase(anno.getAnnotationLabel())){
							existingFeature.setValue(anno.getAnnotationLabel());
						}
					}else{
						log.error("Entity changed in DiscourseDB since the data was last import but also changed in the exported file. Cannot import feature.");							
					}
				} else {
					// feature didn't exist in database yet. Create it.
					DDBEntityInfo referenceAnnotationInfo = annotationBratIdToDDB.get(anno.getSourceAnnotationId());
					if(referenceAnnotationInfo!=null){
						AnnotationInstance referenceAnno = annoService.findOneAnnotationInstance(referenceAnnotationInfo.getId()).get();
						Feature newFeature = annoService.createTypedFeature(anno.getType().name(), anno.getAnnotationLabel());
						annoService.addFeature(referenceAnno, newFeature);
					}else{
						log.error("Cannot find the annotation this feature applies to.");
					}						
				}
			} else {
				//Implement import capabilities for other Brat Annotation types here
				log.error("Unsupported Annotation type " + anno.getType().name()+" Skipping.");
			}
		}
		
		
		// delete remaining annotations and features that don't show up any more in the brat files
		for(Long id:ddbFeatureIds){
			System.out.println("Delete feature "+id);
			annoService.deleteFeature(id); //FIXME does not delete. it does work when called outside of this transaction
		}
		for(Long id:ddbAnnotationIds){
			System.out.println("Delete annotation "+id);
			annoService.deleteAnnotation(id);  //FIXME does not delete. it does work when called outside of this transaction
		}

	}

	private TreeMap<Integer, Long> getOffsetToIdMap(File offsetFile, EntityTypes entityType) throws IOException {
		TreeMap<Integer, Long> offsetToId = new TreeMap<>();
		for (String line : FileUtils.readLines(offsetFile)) {
			String[] fields = line.split("\t");
			if (fields[0].equalsIgnoreCase(entityType.name())) {
				offsetToId.put(Integer.parseInt(fields[2]), Long.parseLong(fields[1]));
			}
		}
		return offsetToId;
	}

	private Map<String, DDBEntityInfo> getBratIdToDdbIdMap(File versionFile, AnnotationSourceType sourceType) throws IOException {
		Map<String, DDBEntityInfo> bratIdToDdbVersion = new HashMap<>();
		for (String line : FileUtils.readLines(versionFile)) {
			String[] fields = line.split("\t");
			if (fields[0].equalsIgnoreCase(sourceType.name())) {
				bratIdToDdbVersion.put(fields[1], new DDBEntityInfo(Long.parseLong(fields[2]),Long.parseLong(fields[3])));
			}
		}
		return bratIdToDdbVersion;
	}
	
	private void extractAnnotationAndFeatureIds(List<AnnotationInstance> annos, Set<Long> annoIds, Set<Long> featureIds){
		for(AnnotationInstance anno:annos){
			annoIds.add(anno.getId());
			if(anno.getFeatures()!=null){
				featureIds.addAll(anno.getFeatures().stream().map(f->f.getId()).collect(Collectors.toList()));
			}
		}
	}
	
	@Data
	@AllArgsConstructor
	protected class DDBEntityInfo{
		Long id;
		Long version;
	}
	
}
