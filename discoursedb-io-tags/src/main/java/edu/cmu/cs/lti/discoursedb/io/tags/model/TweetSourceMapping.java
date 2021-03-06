/*******************************************************************************
 * Copyright (C)  2015 - 2016  Carnegie Mellon University
 * Author: Oliver Ferschke
 * Contributor Haitian Gong
 *
 * This file is part of DiscourseDB.
 *
 * DiscourseDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * DiscourseDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DiscourseDB.  If not, see <http://www.gnu.org/licenses/> 
 * or write to the Free Software Foundation, Inc., 51 Franklin Street, 
 * Fifth Floor, Boston, MA 02110-1301  USA
 *******************************************************************************/
package edu.cmu.cs.lti.discoursedb.io.tags.model;

/**
 * 
 * Defines source descriptors that identifies the mapping from source id to DiscourseDB entity and disambiguates source entities.
 * 
 * e.g. 
 * "contribution#id_str" means that a tweet in source csv file identified by its "id" was translated into a contribution entity.
 * The same "id" with descriptor "content#id_str" is used as unique identifier for the content entity of this tweet. 
 * 
 * The main reason for this is to avoid collision in importing process.
 * 
 * @author Haitian Gong
 * @author Oliver Ferschke
 *
 */
public class TweetSourceMapping {
	public static final String ID_STR_TO_CONTRIBUTION = "contribution#id_str";
	public static final String ID_STR_TO_CONTENT = "content#id_str";
	public static final String FROM_USER_ID_STR_TO_USER= "user#from_user_id_str";
}
