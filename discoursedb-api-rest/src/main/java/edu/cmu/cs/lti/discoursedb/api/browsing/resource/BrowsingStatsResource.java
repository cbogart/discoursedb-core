package edu.cmu.cs.lti.discoursedb.api.browsing.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.hateoas.ResourceSupport;

import edu.cmu.cs.lti.discoursedb.core.model.macro.Discourse;
import edu.cmu.cs.lti.discoursedb.core.repository.macro.ContributionRepository;
import edu.cmu.cs.lti.discoursedb.core.repository.macro.DiscoursePartRepository;
import edu.cmu.cs.lti.discoursedb.core.repository.macro.DiscourseRepository;
import edu.cmu.cs.lti.discoursedb.core.repository.user.UserRepository;

public class BrowsingStatsResource extends ResourceSupport {

	private Map<String,Integer> discourseParts;
	private long users;
	private Map<String,Integer> contributions;
	private List<String> discourses;
	
	public BrowsingStatsResource(DiscourseRepository discourseRepository, 
			DiscoursePartRepository discoursePartRepository, 
			ContributionRepository contributionRepository, 
			UserRepository userRepository) {

		this.discourses = new ArrayList<String>();
		this.discourseParts = new HashMap<String,Integer>();
		this.contributions = new HashMap<String,Integer>();
		this.users = 0;
		
		discourseRepository.findAll().forEach(d -> this.discourses.add(d.getName()));
		this.users = userRepository.count();
		discoursePartRepository.findAll().forEach(dp -> {
			this.discourseParts.put(dp.getType(), this.discourseParts.getOrDefault(dp.getType(), 0)+1);
		});
		
		contributionRepository.findAll().forEach(c -> {
			this.contributions.put(c.getType(), this.contributions.getOrDefault(c.getType(), 0)+1);
		});
		
		
				
				//.map(d -> d.getName()).collect(Collectors.toList());
	}

	public Map<String, Integer> getDiscourseParts() {
		return discourseParts;
	}

	public void setDiscourseParts(Map<String, Integer> discourseParts) {
		this.discourseParts = discourseParts;
	}

	public long getUsers() {
		return users;
	}

	public void setUsers(long users) {
		this.users = users;
	}

	public Map<String, Integer> getContributions() {
		return contributions;
	}

	public void setContributions(Map<String, Integer> contributions) {
		this.contributions = contributions;
	}

	public List<String> getDiscourses() {
		return discourses;
	}

	public void setDiscourses(List<String> discourses) {
		this.discourses = discourses;
	}

	
	
	
	
}
