package edu.cmu.cs.lti.discoursedb.api.browsing.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resources;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import edu.cmu.cs.lti.discoursedb.api.browsing.resource.BrowsingContributionResource;
import edu.cmu.cs.lti.discoursedb.api.browsing.resource.BrowsingDiscoursePartResource;
import edu.cmu.cs.lti.discoursedb.api.browsing.resource.BrowsingStatsResource;
import edu.cmu.cs.lti.discoursedb.api.recommendation.resource.RecommendationContributionResource;
import edu.cmu.cs.lti.discoursedb.api.recommendation.resource.RecommendationDataSourceInstanceResource;
import edu.cmu.cs.lti.discoursedb.api.recommendation.resource.RecommendationDiscoursePartResource;
import edu.cmu.cs.lti.discoursedb.api.recommendation.resource.RecommendationDiscourseResource;
import edu.cmu.cs.lti.discoursedb.api.recommendation.resource.RecommendationUserResource;
import edu.cmu.cs.lti.discoursedb.core.model.macro.Contribution;
import edu.cmu.cs.lti.discoursedb.core.model.macro.Discourse;
import edu.cmu.cs.lti.discoursedb.core.model.macro.DiscoursePart;
import edu.cmu.cs.lti.discoursedb.core.model.macro.DiscourseRelation;
import edu.cmu.cs.lti.discoursedb.core.model.user.User;
import edu.cmu.cs.lti.discoursedb.core.repository.macro.ContributionRepository;
import edu.cmu.cs.lti.discoursedb.core.repository.macro.DiscoursePartContributionRepository;
import edu.cmu.cs.lti.discoursedb.core.repository.macro.DiscoursePartRelationRepository;
import edu.cmu.cs.lti.discoursedb.core.repository.macro.DiscoursePartRepository;
import edu.cmu.cs.lti.discoursedb.core.repository.macro.DiscourseRepository;
import edu.cmu.cs.lti.discoursedb.core.repository.macro.DiscourseToDiscoursePartRepository;
import edu.cmu.cs.lti.discoursedb.core.repository.user.UserRepository;
import edu.cmu.cs.lti.discoursedb.core.type.DiscourseRelationTypes;

@Controller
@RequestMapping(value = "/browsing", produces = "application/hal+json")
public class BrowsingRestController {

	@Autowired
	private DiscourseRepository discourseRepository;

	@Autowired
	private DiscoursePartRepository discoursePartRepository;

	@Autowired
	private DiscoursePartRelationRepository discoursePartRelationRepository;
	
	@Autowired
	private DiscourseToDiscoursePartRepository discourseToDiscoursePartRepository;

	@Autowired
	DiscoursePartContributionRepository discoursePartContributionRepository;
	
	@Autowired
	private ContributionRepository contributionRepository;

	@Autowired
	private UserRepository userRepository;

	@RequestMapping(value="/stats", method=RequestMethod.GET)
	@ResponseBody
	Resources<BrowsingStatsResource> stats() {
		BrowsingStatsResource bsr = new BrowsingStatsResource(discourseRepository, discoursePartRepository, contributionRepository, userRepository);
		List<BrowsingStatsResource> l = new ArrayList<BrowsingStatsResource>();
		l.add(bsr);
		
		Resources<BrowsingStatsResource> r =  new Resources<BrowsingStatsResource>(l);
		for (String t: bsr.getDiscourseParts().keySet()) {
			r.add(makeLink("/browsing/repos?repoType=" + t, t));			
		}
		/*for (String t: bsr.getContributions().keySet()) {
			r.add(makeLink("/browsing/repos?repoType=" + t, t));			
		}*/
		return r;
	}
	
	
	@RequestMapping(value = "/repos", method = RequestMethod.GET)
	@ResponseBody
	Resources<BrowsingDiscoursePartResource> discourseParts(@RequestParam(value= "page", defaultValue = "0") int page, 
														   @RequestParam(value= "size", defaultValue="20") int size,
														   @RequestParam("repoType") String repoType,
														   @RequestParam(value="annoType", defaultValue="*") String annoType) {
		PageRequest p = new PageRequest(page,size);
		Page<BrowsingDiscoursePartResource> repoResources = 
				discoursePartRepository.findAllNonDegenerateByType(repoType, p)
				.map(BrowsingDiscoursePartResource::new)
				.map(bdpr -> {bdpr.filterAnnotations(annoType); return bdpr; });
		
		Resources<BrowsingDiscoursePartResource> response = new Resources<BrowsingDiscoursePartResource>(repoResources);
		if (!repoResources.isFirst()) {	response.add(makePageLink(0, size, Link.REL_FIRST));   }
		if (!repoResources.isLast()) {	response.add(makePageLink(repoResources.getNumberOfElements()-1, size, Link.REL_LAST));   }
		if (repoResources.hasPrevious()) {	response.add(makePageLink(page-1, size, Link.REL_PREVIOUS));   }
		if (repoResources.hasNext()) {	response.add(makePageLink(page+1, size, Link.REL_NEXT));   }
		response.add(makePageLink(page, size, Link.REL_SELF));  
		response.add(makeLink("/browsing/repos{?page,size,repoType,annoType}", "search"));
		return response;
	}
	
	@RequestMapping(value = "/subDiscourseParts/{childOf}", method = RequestMethod.GET)
	@ResponseBody
	Resources<BrowsingDiscoursePartResource> subDiscourseParts(@RequestParam(value= "page", defaultValue = "0") int page, 
														   @RequestParam(value= "size", defaultValue="20") int size,
														   @PathVariable("childOf") Long dpId)  {
		PageRequest p = new PageRequest(page,size);
		
		Optional<DiscoursePart> parent = discoursePartRepository.findOne(dpId);
		if (parent.isPresent()) {
			Page<BrowsingDiscoursePartResource> repoResources = 
					discoursePartRelationRepository.findAllByTarget(parent.get(), p)
			.map(dpr -> dpr.getSource())
			.map(BrowsingDiscoursePartResource::new);
			
			Resources<BrowsingDiscoursePartResource> response = new Resources<BrowsingDiscoursePartResource>(repoResources);
			if (!repoResources.isFirst()) {	response.add(makePageLink(0, size, Link.REL_FIRST));   }
			if (!repoResources.isLast()) {	response.add(makePageLink(repoResources.getNumberOfElements()-1, size, Link.REL_LAST));   }
			if (repoResources.hasPrevious()) {	response.add(makePageLink(page-1, size, Link.REL_PREVIOUS));   }
			if (repoResources.hasNext()) {	response.add(makePageLink(page+1, size, Link.REL_NEXT));   }
			response.add(makePageLink(page, size, Link.REL_SELF));  
			//response.add(makeLink("/browsing/subDiscourseParts{?page,size,repoType,annoType}", "search"));
			return response;
		} else {
			return null;
		}
	}
	
	
	@RequestMapping(value = "/dpContributions/{childOf}", method = RequestMethod.GET)
	@ResponseBody
	Resources<BrowsingContributionResource> dpContributions(@RequestParam(value= "page", defaultValue = "0") int page, 
														   @RequestParam(value= "size", defaultValue="20") int size,
														   @PathVariable("childOf") Long dpId)  {
		PageRequest p = new PageRequest(page,size);
		
		
		Optional<DiscoursePart> parent = discoursePartRepository.findOne(dpId);
		if (parent.isPresent()) {
			List<BrowsingContributionResource> lbcr = parent.get().getDiscoursePartContributions()
					.stream().map(c->c.getContribution())
					.map(BrowsingContributionResource::new)
					.collect(Collectors.toList());
			Page<BrowsingContributionResource> pbcr = 
					new PageImpl<BrowsingContributionResource>( lbcr);
			
			Resources<BrowsingContributionResource> response = new Resources<BrowsingContributionResource>(pbcr);
			if (!pbcr.isFirst()) {	response.add(makePageLink(0, size, Link.REL_FIRST));   }
			if (!pbcr.isLast()) {	response.add(makePageLink(pbcr.getNumberOfElements()-1, size, Link.REL_LAST));   }
			if (pbcr.hasPrevious()) {	response.add(makePageLink(page-1, size, Link.REL_PREVIOUS));   }
			if (pbcr.hasNext()) {	response.add(makePageLink(page+1, size, Link.REL_NEXT));   }
			response.add(makePageLink(page, size, Link.REL_SELF)); 
			
			//response.add(makeLink("/browsing/subDiscourseParts{?page,size,repoType,annoType}", "search"));
			return response;
		} else {
			return null;
		}
	}
    
	public static Link makeLink(String dest, String rel) {
			String path = ServletUriComponentsBuilder.fromCurrentRequestUri()
				.replacePath(dest)
		        .build()
		        .toUriString();
		    Link link = new Link(path,rel);
		    return link;	
	}
	public static Link makePageLink(int page, int size, String rel) {
		String path = ServletUriComponentsBuilder.fromCurrentRequest()
				.replaceQueryParam("page", page)
		        .replaceQueryParam("size",size)
		        .build()
		        .toUriString();
		    Link link = new Link(path,rel);
		    return link;
	 }
}
