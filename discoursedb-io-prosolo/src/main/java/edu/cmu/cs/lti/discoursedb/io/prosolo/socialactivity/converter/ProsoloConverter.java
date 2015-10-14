package edu.cmu.cs.lti.discoursedb.io.prosolo.socialactivity.converter;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import edu.cmu.cs.lti.discoursedb.core.model.macro.Content;
import edu.cmu.cs.lti.discoursedb.core.model.macro.Contribution;
import edu.cmu.cs.lti.discoursedb.core.model.macro.Discourse;
import edu.cmu.cs.lti.discoursedb.core.model.macro.DiscoursePart;
import edu.cmu.cs.lti.discoursedb.core.model.system.DataSourceInstance;
import edu.cmu.cs.lti.discoursedb.core.model.user.User;
import edu.cmu.cs.lti.discoursedb.core.service.macro.ContentService;
import edu.cmu.cs.lti.discoursedb.core.service.macro.ContributionService;
import edu.cmu.cs.lti.discoursedb.core.service.macro.DiscoursePartService;
import edu.cmu.cs.lti.discoursedb.core.service.macro.DiscourseService;
import edu.cmu.cs.lti.discoursedb.core.service.system.DataSourceService;
import edu.cmu.cs.lti.discoursedb.core.service.user.UserInteractionService;
import edu.cmu.cs.lti.discoursedb.core.service.user.UserService;
import edu.cmu.cs.lti.discoursedb.core.type.ContributionInteractionTypes;
import edu.cmu.cs.lti.discoursedb.core.type.ContributionTypes;
import edu.cmu.cs.lti.discoursedb.core.type.DataSourceTypes;
import edu.cmu.cs.lti.discoursedb.core.type.DiscoursePartTypes;
import edu.cmu.cs.lti.discoursedb.io.prosolo.socialactivity.io.ProsoloDB;
import edu.cmu.cs.lti.discoursedb.io.prosolo.socialactivity.model.ProsoloNode;
import edu.cmu.cs.lti.discoursedb.io.prosolo.socialactivity.model.ProsoloPost;
import edu.cmu.cs.lti.discoursedb.io.prosolo.socialactivity.model.ProsoloUser;
import edu.cmu.cs.lti.discoursedb.io.prosolo.socialactivity.model.SocialActivity;

/**
 * This converter loads data from a prosolo database and maps it to DiscourseDB.
 * The DiscourseDB configuration is defined in the dicoursedb-model project and
 * Spring/Hibernate are taking care of connections.
 * 
 * The connection to the prosolo database is more lightweight and uses a JDBC
 * connection. The configuration parameters for this connection are passed to
 * the converter as launch parameters in the following order
 * 
 * <DiscourseName> <DataSetName> <prosolo_dbhost> <prosolo_db> <prosolo_dbuser> <prosolo_dbpwd>
 * 
 * @author Oliver Ferschke
 *
 */
@Component
@Transactional
@Order(1)
public class ProsoloConverter implements CommandLineRunner {

	private static final Logger logger = LogManager.getLogger(ProsoloConverter.class);
	
	/**
	 * List of all supported actions on nodes.
	 */
	private final List<String> NODE_ACTIONS = Arrays.asList(new String[]{"Comment","Create"});
	/**
	 * List of all the actions that create/add a new contribution.
	 */
	private final List<String> CREATE_ACTIONS = Arrays.asList(new String[]{"AddNote","Post"});
	/**
	 * List of all the actions that share a contribution.
	 */
	private final List<String> SHARE_ACTIONS = Arrays.asList(new String[]{"PostShare"});
	
	private String discourseName;
	private DataSourceTypes dataSourceType;
	private String dataSetName;
	
	private ProsoloDB prosolo = null;

	@Autowired private DiscourseService discourseService;	
	@Autowired private UserService userService;
	@Autowired private DataSourceService dataSourceService;
	@Autowired private ContentService contentService;
	@Autowired private ContributionService contributionService;
	@Autowired private DiscoursePartService discoursePartService;
	@Autowired private UserInteractionService userInteractionService;
	
	@Override 
	public void run(String... args) throws Exception {

		if (args.length != 6) {
			logger.error("Incorrect number of parameters. USAGE: <DiscourseName> <DataSetName> <prosolo_dbhost> <prosolo_db> <prosolo_dbuser> <prosolo_dbpwd>");
			System.exit(1);
		}

		//Parse command line parameters		
		this.discourseName=args[0];			
		this.dataSourceType = DataSourceTypes.PROSOLO;
		this.dataSetName=args[1];

		prosolo = new ProsoloDB(args[2],args[3],args[4],args[5]);

		logger.info("Start mapping to DiscourseDB...");
		try {
			map();
		} catch (SQLException ex) {
			logger.error(ex.getMessage(), ex);
		} finally {
			prosolo.closeConnection();
		}
		logger.info("...mapping complete");
	}

	/**
	 * Calls the mapping routines for the different social activity types in ProSolo.
	 * If the provided dataset has previously been imported already, the process will not proceed importing anything to avoid duplicates.
	 *  
	 * @throws SQLException In case of a database access error
	 */
	private void map() throws SQLException {
		if(dataSourceService.dataSourceExists(dataSetName)){
			logger.warn("Dataset "+dataSetName+" has previously already been imported. Terminating...");			
			return;
		}
		mapCreateSocialActivities("PostSocialActivity", "Post");
		mapShareSocialActivities("PostSocialActivity","PostShare");
		mapNodeSocialActivities("NodeSocialActivity", "Create");
		mapCreateSocialActivities("GoalNoteSocialActivity", "AddNote"); //TODO create discourserelation Node-GoalNote
		mapNodeSocialActivities("NodeComment", "Comment"); //TODO create discourserelation Node-NodeComment
	}

	/**
	 * Maps social activities of the given type and the given action to DiscourseDB
	 * This method covers "post" or "add" actions that create new contributions.
	 * 
	 * @param dtype the type of the SocialActivity
	 * @param action the create/add action
	 * @throws SQLException
	 */
	private void mapCreateSocialActivities(String dtype, String action) throws SQLException{
		if(!CREATE_ACTIONS.contains(action)){
			logger.warn("Action "+action+" (SocialActivity type "+dtype+") cannot be mapped to a new contribution. The action is not registered as a create/add action.");
			return;
		}
		
		//We assume here that a single ProSolo database refers to a single course (i.e. a single Discourse)
		//The course details are passed on as a parameter to this converter and are not read from the prosolo database
		Discourse discourse = discourseService.createOrGetDiscourse(this.discourseName);
		
		for (Long l : prosolo.getIdsForDtypeAndAction(dtype, action)) {
			logger.trace("Processing "+dtype+" ("+action+") id:"+l);			

			//get data from prosolo database
			SocialActivity curPostActivity = prosolo.getSocialActivity(l).get(); 
			ProsoloPost curProsoloPost = prosolo.getProsoloPost(curPostActivity.getPost_object()).get();			
			
			// each social activity translates to a separate DiscoursePart			
			DiscoursePart postSocialActivityContainer = discoursePartService.createOrGetTypedDiscoursePart(discourse, lookUpDiscoursePartType(dtype));		
			
			ProsoloUser curProsoloUser = prosolo.getProsoloUser(curPostActivity.getMaker()).get();
			User curUser = addOrUpdateUser(curProsoloUser);
			
			Content curContent = contentService.createContent();
			curContent.setAuthor(curUser);
			curContent.setStartTime(curProsoloPost.getCreated());
			curContent.setText(curProsoloPost.getContent());			
			dataSourceService.addSource(curContent, new DataSourceInstance(curProsoloPost.getId()+"",dataSourceType,dataSetName));
			
			Contribution curContrib = contributionService.createTypedContribution(lookUpContributionType(dtype));
			curContrib.setCurrentRevision(curContent);
			curContrib.setFirstRevision(curContent);
			curContrib.setStartTime(curProsoloPost.getCreated());
			curContrib.setUpvotes(curPostActivity.getLike_count());			
			curContrib.setDownvotes(curPostActivity.getDislike_count());			
			dataSourceService.addSource(curContrib, new DataSourceInstance(curProsoloPost.getId()+"",dataSourceType,dataSetName));
		
			//add contribution to DiscoursePart
			discoursePartService.addContributionToDiscoursePart(curContrib, postSocialActivityContainer);			
		}
	}
	
	/**
	 * Maps social activities of the given type and the given action to DiscourseDB
	 * This method only covers "create" or "comment" actions related to Node contributions.
	 * 
	 * @param dtype the type of the SocialActivity
	 * @param action the create/add action
	 * @throws SQLException
	 */
	private void mapNodeSocialActivities(String dtype, String action) throws SQLException{
		if(!NODE_ACTIONS.contains(action)){
			logger.warn("Action "+action+" (SocialActivity type "+dtype+") cannot be mapped to a new contribution. The action is not registered as a node action.");
			return;
		}
		
		//We assume here that a single ProSolo database refers to a single course (i.e. a single Discourse)
		//The course details are passed on as a parameter to this converter and are not read from the prosolo database
		Discourse discourse = discourseService.createOrGetDiscourse(this.discourseName);
		
		for (Long l : prosolo.getIdsForDtypeAndAction(dtype, action)) {
			logger.trace("Processing "+dtype+" ("+action+") id:"+l);			

			//get data from prosolo database
			SocialActivity curNodeActivity = prosolo.getSocialActivity(l).get(); 
			Optional<ProsoloNode> existingNode = prosolo.getProsoloNode(curNodeActivity.getNode());
			
			if(existingNode.isPresent()){
				ProsoloNode curNode=existingNode.get();			
			
				// each social activity translates to a separate DiscoursePart			
				DiscoursePart nodeSocialActivityContainer = discoursePartService.createOrGetTypedDiscoursePart(discourse, lookUpDiscoursePartType(dtype));		
				
				ProsoloUser curProsoloUser = prosolo.getProsoloUser(curNodeActivity.getMaker()).get();
				User curUser = addOrUpdateUser(curProsoloUser);						 								
				
				Content curContent = contentService.createContent();
				curContent.setAuthor(curUser);
				curContent.setStartTime(curNode.getCreated());
				curContent.setTitle(curNode.getTitle());
				curContent.setTitle(curNode.getDc_description());
				dataSourceService.addSource(curContent, new DataSourceInstance(curNode.getId()+"",dataSourceType,dataSetName));
				
				Contribution curContrib = contributionService.createTypedContribution(lookUpContributionType(curNode.getDtype()));
				curContrib.setCurrentRevision(curContent);
				curContrib.setFirstRevision(curContent);
				curContrib.setStartTime(curNode.getCreated());
				curContrib.setUpvotes(curNodeActivity.getLike_count());
				curContrib.setDownvotes(curNodeActivity.getDislike_count());
				curContrib.setStartTime(curNode.getCreated());
				dataSourceService.addSource(curContrib, new DataSourceInstance(curNode.getId()+"",dataSourceType,dataSetName));
				
				//add contribution to DiscoursePart
				discoursePartService.addContributionToDiscoursePart(curContrib, nodeSocialActivityContainer);
				
				
				//if type is NodeComment.....
				
			}
			
		}
	}
	
	
	/**
	 * Maps social activities of the given type and the given action to DiscourseDB
	 * This method only covers "share" actions that create new UserContribution interactions.
	 * 
	 * 
	 * @param dtype the type of social activity
	 * @param action the share action
	 * @throws SQLException
	 */
	private void mapShareSocialActivities(String dtype, String action) throws SQLException{
		if(!SHARE_ACTIONS.contains(action)){
			logger.warn("Action "+action+" (SocialActivity type "+dtype+") cannot be mapped to a share interaction. The action is not registered as a share action.");
			return;
		}

		for (Long l : prosolo.getIdsForDtypeAndAction(dtype, action)) {
			logger.trace("Processing "+dtype+" ("+action+") id:"+l);			

			SocialActivity curSharingActivity = prosolo.getSocialActivity(l).get();
			ProsoloPost sharingPost = prosolo.getProsoloPost(curSharingActivity.getPost_object()).get();
				
			//get the entity that was shared
			if(sharingPost.getReshare_of()!=null){
				Optional<ProsoloPost> existingSharedPost = prosolo.getProsoloPost(sharingPost.getReshare_of());
				if(existingSharedPost.isPresent()){
					ProsoloPost sharedPost = existingSharedPost.get();
					//look up the contribution for the shared entity in DiscourseDB
					Optional<Contribution> sharedContribution = contributionService.findOneByDataSource(sharedPost.getId()+"", dataSetName);
					if(sharedContribution.isPresent()){					
						ProsoloUser sharingProsoloUser = prosolo.getProsoloUser(curSharingActivity.getMaker()).get();
						userInteractionService.createTypedContributionIteraction(addOrUpdateUser(sharingProsoloUser), sharedContribution.get(), ContributionInteractionTypes.SHARE);					
						//TODO in addition to the userInteraction, should the shared entity also be added as a contribution? Shares can have likes.		
						//There are PostShare entities that don't have a reshare_of value. Unclear how they have to be interpreted
					}										
				}
			}
		}		
	}
	
	/**
	 * Creates a new DiscourseDB user based on the information in the ProsoloUser object if it doesn't exist
	 * or updates the contents of an existing DiscourseDB user.
	 * 
	 * @param prosoloUser the prosolo user to add to discoursedb 
	 * @return the DiscourseDB user based on or updated with the prosolo user
	 * @throws SQLException In case of a database access error
	 */
	private User addOrUpdateUser(ProsoloUser prosoloUser) throws SQLException{
		User curUser = null; 
		if(prosoloUser==null){
			logger.error("Could not find user information for prosolo user in prosolo database");
		}else{				
			//CHECK IF USER WITH SAME edX username exists in the current Discourse context
			Optional<String> edXUserName = prosolo.mapProsoloUserIdToedXUsername(prosoloUser.getId());
			if(edXUserName.isPresent()){
				curUser=userService.createOrGetUser(discourseService.createOrGetDiscourse(this.discourseName), edXUserName.get());
				dataSourceService.addSource(curUser, new DataSourceInstance(prosoloUser.getId()+"",dataSourceType,dataSetName));
			}else{
				curUser=userService.createOrGetUser(discourseService.createOrGetDiscourse(this.discourseName),"", prosoloUser.getId()+"",dataSourceType,dataSetName);
			}

			//update the real name of the user if necessary
			curUser=userService.setRealname(curUser, prosoloUser.getName(), prosoloUser.getLastname());
			
			//Update email address if not set in db (TODO allow multiple email addresses?)
			if(curUser.getEmail()==null||curUser.getEmail().isEmpty()){
				Optional<String> prosoloMail = prosolo.getEmailForProsoloUser(prosoloUser.getId());
				if(prosoloMail.isPresent()){
					curUser.setEmail(prosoloMail.get());
				}
			}
			
			//Update location if not yet set in db
			if(curUser.getLocation()==null||curUser.getLocation().isEmpty()){
				curUser.setLocation(prosoloUser.getLocation_name());					
			}

			//add new data source
			dataSourceService.addSource(curUser, new DataSourceInstance(prosoloUser.getId()+"",dataSourceType,dataSetName));
		}
		return curUser;		
	}
	
	
	/**
	 * Maps SocialActivity dtype to an appropriate DiscourseDB ContributionType
	 * 
	 * @param dtype the SocialActivity type
	 * @return an appropriate ContributionType for the given SocialActivity entity
	 */
	private ContributionTypes lookUpContributionType(String dtype){
		switch(dtype){
			case "GoalNoteSocialActivity": return ContributionTypes.GOAL_NOTE; 
			case "LearningGoal": return ContributionTypes.LEARNING_GOAL; 
			case "TargetLearningGoal": return ContributionTypes.LEARNING_GOAL;  
			case "Competence": return ContributionTypes.COMPETENCE; 
			case "TargetCompetence": return ContributionTypes.COMPETENCE;  
			case "ResourceActivity": return ContributionTypes.RESOURCE_ACTIVITY;
			case "TargetActivity": return ContributionTypes.TARGET_ACTIVITY; 
			case "UploadAssignmentActivity": return ContributionTypes.UPLOAD_ASSIGNMENT_ACTIVITY;
			case "NodeComment": return ContributionTypes.NODE_COMMENT; 
			case "PostSocialActivity": return ContributionTypes.POST; 
			default: throw new IllegalArgumentException("No ContributionType mapping for dtype "+dtype); 
		}
	}

	/**
	 * Maps SocialActivity dtype to an appropriate DiscourseDB DiscoursePartType
	 * 
	 * @param dtype the SocialActivity type
	 * @return an appropriate DiscoursePartType for the given SocialActivity entity
	 */
	private DiscoursePartTypes lookUpDiscoursePartType(String dtype){
		switch(dtype){
			case "GoalNoteSocialActivity": return DiscoursePartTypes.PROSOLO_GOAL_NOTE_SOCIAL_ACTIVITY;
			case "NodeSocialActivity": return DiscoursePartTypes.PROSOLO_NODE_SOCIAL_ACTIVITY;
			case "NodeComment": return DiscoursePartTypes.PROSOLO_NODE_SOCIAL_ACTIVITY;
			case "PostSocialActivity": return DiscoursePartTypes.PROSOLO_POST_SOCIAL_ACTIVITY;			
			default: throw new IllegalArgumentException("No DiscoursePartType mapping for dtype "+dtype);
		}
	}

}