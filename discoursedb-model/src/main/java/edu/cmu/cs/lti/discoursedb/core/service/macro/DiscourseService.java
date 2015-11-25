package edu.cmu.cs.lti.discoursedb.core.service.macro;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.cmu.cs.lti.discoursedb.core.model.macro.Discourse;
import edu.cmu.cs.lti.discoursedb.core.model.macro.DiscoursePart;
import edu.cmu.cs.lti.discoursedb.core.model.macro.QDiscourse;
import edu.cmu.cs.lti.discoursedb.core.repository.macro.DiscourseRepository;

@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
@Service
public class DiscourseService {

	@Autowired
	private DiscourseRepository discourseRepository;

	/**
	 * Returns a Discourse object with the given name if it exists or creates a
	 * new Discourse entity with that name, stores it in the database and
	 * returns the entity object.
	 * 
	 * @param name
	 *            name of the requested discourse
	 * @return the Discourse object with the given name - either retrieved or
	 *         newly created
	 */
	public Discourse createOrGetDiscourse(String name) {
		Optional<Discourse> curOptDiscourse = discourseRepository.findOneByName(name);
		Discourse curDiscourse;
		if (curOptDiscourse.isPresent()) {
			curDiscourse = curOptDiscourse.get();
		} else {
			curDiscourse = new Discourse(name);
			curDiscourse = discourseRepository.save(curDiscourse);
		}
		return curDiscourse;
	}

	/**
	 * Finds one DiscoursePart of the given type, with the given name and associated with the given discourse
	 *  
	 * @param discoursePart the DiscoursePart for which the discourse should be retrieved
	 * @return and Optional that contains a Discourse if it exists
	 */
	public Optional<Discourse> findOne(DiscoursePart discoursePart){		
		return Optional.ofNullable(discourseRepository
				.findOne(QDiscourse.discourse.discourseToDiscourseParts.any().discoursePart.eq(discoursePart)));
	}
	
	public Discourse findOne(Long id){
		return discourseRepository.findOne(id);
	}
	
	/**
	 * Calls the save method of the Discourse repository to save the given
	 * Discourse object to the DB. Returns the Discourse object after the save
	 * process.
	 * 
	 * @param discourse
	 *            the Discourse object to save
	 * @return the (potentially altered) Discourse object that is returned after
	 *         the save process
	 */
	public Discourse save(Discourse discourse) {
		return discourseRepository.save(discourse);
	}
	
	

}
